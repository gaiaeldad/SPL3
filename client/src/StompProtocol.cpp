#include "StompProtocol.h"
#include "../include/ConnectionHandler.h"
#include <sstream>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <algorithm>
#include <iomanip>
#include <mutex>
#include <condition_variable>
#include <shared_mutex>
#include <boost/thread/shared_mutex.hpp>

using namespace std;

//keybordThred


void StompProtocol::handleLogin(const string& hostPort, const string& username, const string& password) {
     boost::unique_lock<boost::shared_mutex> lock(isConnectedMutex);
     cout << "got to handle login"<< endl;

    if (connected) {
        cerr << "The client is already logged in, log out before trying again." << endl;
        return;
    }
    cout << "got to 1"<< endl;
    size_t colonPos = hostPort.find(':');
    cout << "got to 2"<< endl;
    if (colonPos == string::npos) {
        throw runtime_error("Invalid host:port format");
    }
    cout << "got to 3"<< endl;
    string host = hostPort.substr(0, colonPos);
    short port = static_cast<short>(stoi(hostPort.substr(colonPos + 1)));

    if (host.empty() ||  username.empty() || password.empty()) {//|| port.empty()
        std::cerr << "Missing one or more arguments. Expected <host:port> <username> <password>." << std::endl;
        return;
    }

    try{
        // עדכון המצביע המשותף
        cout << "tring to create connection hendler"<<  host << port << endl;
        CH = make_shared<ConnectionHandler>(host, port);
    }
    catch (const std::exception &e) {
        std::cerr << "[Error] Invalid port number: " << e.what() << std::endl;
        return;
    }
    cout << "create connection hendler"<<  host << port << endl;
    cout << "Checking condition: connectionHandler is " << (CH ? "initialized" : "nullptr") << endl;

    if(!CH->connect()){
        std::cerr << "Coulden't connect to server...." << std::endl;
        CH.reset();
        return;
    }

    this->username = username;

    string frame = "CONNECT\naccept-version:1.2\nhost:" + host +
                        "\nlogin:" + username +
                        "\npasscode:" + password + "\n\n\0";

    if (!CH->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "[Error] Failed to send CONNECT frame." << std::endl;
        CH.reset();
        return;
    }
    connected = true;
    cout << "Checking condition:2 connectionHandler is " << (CH ? "initialized" : "nullptr") << endl;


}



void StompProtocol::handleJoin(const string& topic) {

    boost::shared_lock<boost::shared_mutex> readLock(isConnectedMutex); // נעילה לקריאה בלבד
    
    if (!connected) {
        cerr << "User is not logged in, can't join: " << topic << endl;
        return;
    }

    boost::unique_lock<boost::shared_mutex> writeLock(receiptCallbacksMutex); // נעילה לכתיבה
    if (topicToSubscriptionId.find(topic) != topicToSubscriptionId.end()) {
        cerr << "Already subscribed to topic: " << topic << endl;
        return;
    }

    nextSubscriptionId++;
    topicToSubscriptionId[topic] = nextSubscriptionId;
    receiptCallbacks[nextReceiptId] = "Joined topic: " + topic;

     boost::unique_lock<boost::shared_mutex> mapWriteLock(eventSummaryMapMutex);
        if (eventSummaryMap.find(topic) == eventSummaryMap.end()) {
            // אם הערוץ לא קיים במפה, יוצרים ערוץ חדש
            eventSummaryMap[topic] = map<string, vector<EmergencyEvent>>();
            std::cout << "[Debug] Created new topic in eventSummaryMap: " << topic << std::endl;
        }

        // מוסיפים את המשתמש למפה תחת הערוץ
        if (eventSummaryMap[topic].find(username) == eventSummaryMap[topic].end()) {
            eventSummaryMap[topic][username] = vector<EmergencyEvent>();
            cout << "[Debug] Added user: " << username << " to topic: " << topic << endl;
        }

        cout << "[Debug] eventSummaryMap after update for topic: " << topic << std::endl;
        for (const auto& channel : eventSummaryMap) {
            std::cout << "  Channel: " << channel.first << std::endl;
            for (const auto& user : channel.second) {
                std::cout << "    User: " << user.first << ", Number of events: " << user.second.size() << std::endl;
            }
        }
// יצירת פריים SUBSCRIBE
    string frame = "SUBSCRIBE\ndestination:" + topic +
                   "\nid:" + to_string(nextSubscriptionId) +
                   "\nreceipt:" + to_string(nextReceiptId) +
                   "\n\n\0";

    nextReceiptId++;
    
    if (!CH->sendFrameAscii(frame, '\0'))
    {
        cerr << "Failed to send SUBSCRIBE frame for channel: " << topic << endl;
        return;
    }
}


void StompProtocol::handleExit(const string& topic) {
     boost::shared_lock<boost::shared_mutex> readLock(isConnectedMutex); // נעילה לקריאה בלבד

    if (!connected) {
        cerr << "User is not logged in, can't exit: " << topic << endl;
        return;
    }
 boost::unique_lock<boost::shared_mutex> writeLock(receiptCallbacksMutex); // נעילה לכתיבה

    if (topicToSubscriptionId.find(topic) == topicToSubscriptionId.end()) {
        cerr << "Not subscribed to topic: " << topic << endl;
        return;
    }

    int subscriptionId = topicToSubscriptionId[topic];
    topicToSubscriptionId.erase(topic);

     // יצירת פריים UNSUBSCRIBE
    std::string frame = "UNSUBSCRIBE\nid:" + std::to_string(subscriptionId) +
                        "\nreceipt:" + std::to_string(nextReceiptId) + "\n\n\0";
    if (!CH->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "Failed to send UNSUBSCRIBE frame for channel: " << topic << std::endl;
        return;
    }
    receiptCallbacks[nextReceiptId] = "Exited topic: " + topic;
    nextReceiptId++;
    return;
}


void StompProtocol::handleReport(const string& filePath) {
     boost::shared_lock<boost::shared_mutex> lock(isConnectedMutex); // נועל לקריאה בלבד

    if (!connected) {
        cerr << "User is not logged in, can't report." << endl;
        return;
    }
    string file = Frame::trim(filePath);

    if (file.empty())
    {
        std::cerr << "File path is empty or invalid." << std::endl;
        return;
    }

    // קריאת קובץ האירועים
    names_and_events parsedData = parseEventsFile(file);
    const string &channelName = parsedData.channel_name; // שם הערוץ מתוך הקובץ
    const vector<Event> &events = parsedData.events;

    if (channelName.empty()) {
        std::cerr << "Channel name is missing in the events file." << std::endl;
        return;
    }

    if (events.empty()) {
        std::cerr << "The events file is empty or invalid." << std::endl;
        return;
    }

    // שליחת כל אירוע כ-FRAME SEND
    for (const Event &event : events) {
        std::ostringstream bodyStream;

        // בניית גוף ההודעה
        bodyStream << "user:" << getUsername() << "\n"
                   << "city:" << event.get_city() << "\n"
                   << "event name:" << event.get_name() << "\n"
                   << "date time:" << event.get_date_time() << "\n"
                   << "general information:\n";

        for (const auto &pair : event.get_general_information()) {
            bodyStream << pair.first << ":" << pair.second << "\n";
        }

        bodyStream << "description:\n"
                   << event.get_description() << "\n";

        // יצירת ה-SEND frame
        std::string frame = "SEND\ndestination:" + channelName + "\n\n" + bodyStream.str() + "\0";

        // שליחת ה-frame
        if (!CH->sendFrameAscii(frame, '\0')) {
            std::cerr << "Failed to send frame for event: " << event.get_name() << std::endl;
        }
    }
}

void StompProtocol::handleLogout() {
    boost::unique_lock<boost::shared_mutex> connectionLock(isConnectedMutex); // נעילה לכתיבה
    if (!connected) {
        cerr << "User is not connected to the server." << endl;
        return;
    }
    // יצירת פריים DISCONNECT
    connected = false;
    string frame = "DISCONNECT\nreceipt:" + to_string(nextReceiptId) + "\n\n\0"; 
    if (!CH->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "Failed to send DISCONNECT frame to the server." << std::endl;
        return;
    }
    boost::unique_lock<boost::shared_mutex> receiptLock(receiptCallbacksMutex);
    receiptCallbacks[nextReceiptId] = "Logged out.";
    nextReceiptId++;
    // connected = false;
}

void StompProtocol::createSummary(const string& channel_name, const string& user, const string& file) {
    boost::shared_lock<boost::shared_mutex> connectLock(isConnectedMutex); // נועל לקריאה בלבד
    if (!connected) {
        std::cerr << "[Error] Not connected. Please connect first." << std::endl;
        return;
    }

    boost::shared_lock<boost::shared_mutex> reportLock(eventSummaryMapMutex); // נועל לקריאה
    string channel = Frame::trim(channel_name);
    string username = Frame::trim(user);
    string fileName = Frame::trim(file);

    if (channel.empty() || username.empty() || fileName.empty()) {
        std::cerr << "[Error] Missing arguments. Usage: summary <channel_name> <user> <file>" << std::endl;
        return;
    }

    // בדיקה אם הערוץ והמשתמש קיימים במפת הסיכומים
    if (eventSummaryMap.find(channel) == eventSummaryMap.end() || eventSummaryMap[channel].find(username) == eventSummaryMap[channel].end()) {
        std::cerr << "[Error] No reports found for channel: " << channel << " and user: " << username << std::endl;
        return;
    }

    const auto &events = eventSummaryMap[channel][username];
    int totalReports = events.size();
    int activeCount = 0, forcesArrivalCount = 0;

    // חישוב סטטיסטיקות ומיון האירועים
    std::vector<EmergencyEvent> sortedEvents = events;
    std::sort(sortedEvents.begin(), sortedEvents.end());

    for (const auto &event : events) {
        const auto &generalInfo = event.get_general_information();

         if (generalInfo.find("active") != generalInfo.end() && generalInfo.at("active") == "true")
        {
            activeCount++;
        }

        if (generalInfo.find("forces_arrival_at_scene") != generalInfo.end() && generalInfo.at("forces_arrival_at_scene") == "true")
        {
            forcesArrivalCount++;
        }

        // if (event.getActive()) {
        //     activeCount++;
        // }

        // if (event.getForcesArrival()) {
        //     forcesArrivalCount++;
        // }
    }

    // פתיחת קובץ לכתיבה
    std::ofstream outputFile(fileName);
    if (!outputFile.is_open()) {
        std::cerr << "[Error] Failed to open file: " << fileName << std::endl;
        return;
    }

    // כתיבה לחלק העליון של הדוח
    outputFile << "Channel " << channel << "\n";
    outputFile << "Stats:\n";
    outputFile << "Total: " << totalReports << "\n";
    outputFile << "active: " << activeCount << "\n";
    outputFile << "forces arrival at scene: " << forcesArrivalCount << "\n";
    outputFile << "Event Reports:\n";

    if (!outputFile) {
        std::cerr << "[Error] Error writing to file: " << fileName << std::endl;
        outputFile.close();
        return;
    }

    // כתיבת האירועים
    for (size_t i = 0; i < sortedEvents.size(); ++i) {
        const auto &event = sortedEvents[i];

        outputFile << "Report_" << i + 1 << ":\n";
        outputFile << "city: " << event.get_city() << "\n";
        outputFile << "date time: " << event.getFormatedDateTime() << "\n";
        outputFile << "event name: " << event.get_name() << "\n";
        outputFile << "summary: " << (event.get_description().size() > 27 ? event.get_description().substr(0, 27) + "..." : event.get_description()) << "\n";

        if (!outputFile) {
            std::cerr << "[Error] Error writing event report to file: " << fileName << std::endl;
            break;
        }
    }

    // סגירת הקובץ
    outputFile.close();
    if (!outputFile) {
        std::cerr << "[Error] Error finalizing the file: " << fileName << std::endl;
    } else {
        std::cout << "Summary successfully written to " << fileName << std::endl;
    }
}


///serverReaderThred

void StompProtocol::handleReceipt(Frame connectFrame)
{
    boost::shared_lock<boost::shared_mutex> lock(receiptCallbacksMutex);

    std::string receiptID = connectFrame.getHeader("receipt-id");

    try
    {
        int receipt = std::stoi(receiptID);

        if (receiptCallbacks.find(receipt) != receiptCallbacks.end())
        {
            std::string output = receiptCallbacks[receipt];
            std::cout << output << std::endl;
            if (output == "Logedout")
            {
                if (CH)
                {
                    CH->close();
                    CH.reset();
                }
            }
        }
        else
        {
            std::cerr << "[Error] Receipt-id not found in receiptCallbacks." << std::endl;
        }
    }
    catch (const std::exception &e)
    {
        std::cerr << "[Error] Failed to parse receipt-id: " << e.what() << std::endl;
    }
}

void StompProtocol::handleMessage(Frame messageFrame) {
    try {
        boost::unique_lock<boost::shared_mutex> lock(eventSummaryMapMutex);

        // שליפת ה-destination וה-body מתוך ה-Frame
        string destination = messageFrame.getHeader("destination");
        string body = messageFrame.getBody();
        Event event(body);

       // הסרת "/" אם מופיע בתחילת ה-destination
        if (!destination.empty() && destination[0] == '/') {
            destination = destination.substr(1);
        }

        // המרת Event ל-EmergencyEvent
        EmergencyEvent emergencyEvent(event);
        string username = emergencyEvent.getEventOwnerUser();

        // הבטחת קיום mutex עבור הערוץ (במידת הצורך)
        ensureChannelMutexExists(destination);
        auto channelMutex = channelMutexes[destination];

        // נעילת הערוץ לעדכון ה-eventSummaryMap
        {
             std::unique_lock<std::mutex> channelLock(*channelMutex);
        for (auto &userEntry : eventSummaryMap[destination]) {
            userEntry.second.push_back(emergencyEvent);
             std::cout << "Event added for user: " << userEntry.first << std::endl;

        }
            // eventSummaryMap[destination][username].push_back(emergencyEvent);
        }


        // הדפסת מידע כללי לצורכי דיבוג
        const auto &generalInfo = emergencyEvent.get_general_information();
        for (const auto &entry : generalInfo) {
            std::cout << "General Info Key: " << entry.first << ", Value: " << entry.second << std::endl;
        }
    }
    catch (const std::exception &e) {
        std::cerr << "[Error] Exception in handleMessage: " << e.what() << std::endl;
    }
    catch (...) {
        std::cerr << "[Error] Unknown error occurred in handleMessage." << std::endl;
    }
}

void StompProtocol::handleError(const Frame &errorFrame){
    boost::unique_lock<boost::shared_mutex> lock(isConnectedMutex);
    cout << "ERROR" << endl;

    for (const auto &header : errorFrame.getHeaders()){
        cout << header.first << ": " << header.second << endl;
    }

    cout << "\nThe message:\n-----" << endl;
    cout << errorFrame.getBody() << endl;

    if (CH != nullptr){
        CH->close();
        CH.reset(); // שחרור הזיכרון
    }

    connected = false;
    setShouldTerminate(true);
}


StompProtocol::StompProtocol(std::shared_ptr<ConnectionHandler> &handler)
    : isConnectedMutex(), eventSummaryMapMutex(), receiptCallbacksMutex(),
      CH(handler), username(""), connected(false), nextSubscriptionId(1), nextReceiptId(1),
      topicToSubscriptionId(), receiptCallbacks(), eventSummaryMap(), shouldTerminate(false) {
}

StompProtocol::~StompProtocol() = default;

void StompProtocol::keyboardLoop(const string& input) {
    Frame frame;
    vector<string> tokens = splitString(input, ' ');

    if (tokens.empty()) return;

    try {
        if (tokens[0] == "login") {
            if (tokens.size() != 4) {
                cout << "Usage: login <host:port> <username> <password>" << endl;
                return;
            }
            handleLogin(tokens[1], tokens[2], tokens[3]);
        } else if (tokens[0] == "join") {
            if (tokens.size() != 2) {
                cout << "Usage: join <topic>" << endl;
                return;
            }
            handleJoin(tokens[1]);
        } else if (tokens[0] == "exit") {
            if (tokens.size() != 2) {
                cout << "Usage: exit <topic>" << endl;
                return;
            }
            handleExit(tokens[1]);
        } else if (tokens[0] == "report") {
            if (tokens.size() != 2) {
                cout << "Usage: report <file>" << endl;
                return;
            }
            handleReport(tokens[1]);
        } else if (tokens[0] == "logout") {
            handleLogout();
        } else if (tokens[0] == "summary") {
            if (tokens.size() != 4) {
                cout << "Usage: summary <channel_name> <user> <file>" << endl;
                return;
            }
            createSummary(tokens[1], tokens[2], tokens[3]);
        } else {
            cout << "Unknown command" << endl;
        }

    } catch (const exception& e) {
        cerr << "Error: " << e.what() << endl;
    }
}


void StompProtocol::handleServerResponse(const string &recivedResponse){ //------------------come back
     // הפיכת ה-STRING ל-FRAME
        Frame response = Frame::parseFrame(recivedResponse);
        handleFrame(response);
}

void StompProtocol::handleFrame(const Frame& response) {
    const string &command = response.getCommand();
    if (command == "CONNECTED") {
        connected = true;
        cout << "Login successful." << endl;
    } else if (command == "RECEIPT") { //--------------------------------------- come back---
        handleReceipt(response);
    } else if (command == "ERROR") {
        handleError(response);

    } 
    else if (command == "MESSAGE") {
         handleMessage(response);
    }
    else {
        cerr << "Unexpected frame received: " << response.command << endl;
    }
}

bool StompProtocol::isshouldTerminate(){
    return shouldTerminate;
}

void StompProtocol::setShouldTerminate(bool value) {
    shouldTerminate = value;
}

vector<string> StompProtocol::splitString(const string& str, char delimiter) {
    vector<string> tokens;
    string token;
    istringstream tokenStream(str);
    while (getline(tokenStream, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}

string StompProtocol::getUsername() const{
    return username;
}

bool StompProtocol::IsConnected()
{
    return connected;
}

