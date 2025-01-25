#include "../include/StompProtocol.h"
#include "../include/ConnectionHandler.h"
#include <iostream>
#include <sstream>

StompProtocol::StompProtocol(std::shared_ptr<ConnectionHandler> &handler)
    : connectionHandler(handler), // הפניה לאותו shared_ptr ב-Main
      isConnectedMutex(),
      reportFromOtherUsersMutex(),
      receiptToStringMutex(),
      isConnected(false),
      myUsername(""),
      subscriptions(),
      reportFromOtherUsers(),
      receiptToString(),
      subscriptionId(1),
      receiptId(1),
      terminateClient(false)
{
}

// ===============================================================================================================================
// עיבוד קלט מהמקלדת
void StompProtocol::processKeyboard(string input)
{
    // ניקוי רווחים בקלט הראשוני
    input = trim(input);

    std::istringstream iss(input);
    string command;
    iss >> command; // קורא את המילה הראשונה מתוך השורה

    string remainingInput;
    std::getline(iss, remainingInput);     // שומר את שאר השורה
    remainingInput = trim(remainingInput); // ניקוי רווחים מיותרים

    // טיפול לפי הפקודה
    if (command == "login")
    {
        processLogin(remainingInput);
    }
    else if (command == "join")
    {
        processJoin(remainingInput);
    }
    else if (command == "exit")
    {
        processExit(remainingInput);
    }
    else if (command == "report")
    {
        processReport(remainingInput);
    }
    else if (command == "summary")
    {
        processSummary(remainingInput);
    }
    else if (command == "logout")
    {
        logout();
    }
    else
    {
        std::cerr << "[Error] Unknown command: " << command << std::endl;
    }
}

// ===============================================================================================================================
void StompProtocol::processLogin(const string &input)
{

    boost::unique_lock<boost::shared_mutex> lock(isConnectedMutex);
    if (isConnected)
    {
        std::cerr << "[Error] Already connected." << std::endl;
        return;
    }

    std::string sanitizedInput = trim(input);

    std::istringstream iss(sanitizedInput);
    std::string hostNport, username, password;
    std::getline(iss, hostNport, ' ');
    std::getline(iss, username, ' ');
    std::getline(iss, password);

    hostNport = trim(hostNport);
    username = trim(username);
    password = trim(password);

    if (hostNport.empty() || username.empty() || password.empty())
    {
        std::cerr << "[Error] Missing one or more arguments." << std::endl;
        return;
    }

    size_t colonPos = hostNport.find(":");
    if (colonPos == std::string::npos)
    {
        std::cerr << "[Error] Invalid host:port format. hostNport='" << hostNport << "'" << std::endl;
        return;
    }

    std::string host = hostNport.substr(0, colonPos);
    std::string port = hostNport.substr(colonPos + 1);

    if (host.empty() || port.empty())
    {
        std::cerr << "[Error] Host or port is empty. host='" << host << "', port='" << port << "'" << std::endl;
        return;
    }

    try
    {
        int portNumber = std::stoi(port);

        // עדכון המצביע המשותף
        connectionHandler = std::make_shared<ConnectionHandler>(host, portNumber);
    }
    catch (const std::exception &e)
    {
        std::cerr << "[Error] Invalid port number: " << e.what() << std::endl;
        return;
    }

    if (!connectionHandler->connect())
    {
        std::cerr << "[Error] Couldn't connect to server." << std::endl;
        connectionHandler.reset();
        return;
    }

    myUsername = username;

   std::string frame = "CONNECT\naccept-version:1.2\nhost:" + host +
                        "\nlogin:" + username +
                        "\npasscode:" + password + "\n\n\0";


    if (!connectionHandler->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "[Error] Failed to send CONNECT frame." << std::endl;
        connectionHandler.reset();
        return;
    }

    isConnected = true;
}

// ===============================================================================================================================
// הצטרפות לערוץ
void StompProtocol::processJoin(const string &input)
{
    boost::shared_lock<boost::shared_mutex> readLock(isConnectedMutex); // נעילה לקריאה בלבד

    if (isConnected == false)
    {
        std::cerr << "Please connect first." << std::endl;
        return;
    }

    boost::unique_lock<boost::shared_mutex> writeLock(receiptToStringMutex); // נעילה לכתיבה

    std::string sanitizedInput = trim(input); // ניקוי רווחים

    if (sanitizedInput.empty())
    {
        std::cerr << "Missing the argument. Expected <channel>." << std::endl;
        return;
    }

    std::istringstream iss(sanitizedInput);
    string channel;

    // קריאה מדויקת של הקלט
    std::getline(iss, channel, ' ');
    channel = trim(channel);

    if (channel.empty())
    {
        std::cerr << "Missing the argument. Expected <channel>." << std::endl;
        return;
    }

    if (iss.rdbuf()->in_avail() > 0)
    { // בדיקה אם נותרו נתונים נוספים בזרם
        std::cerr << "Illegal input length. Expected one argument." << std::endl;
        return;
    }

    if (subscriptions.find(channel) != subscriptions.end())
    {
        std::cerr << "You have already subscribed to this channel." << std::endl;
        return;
    }

    subscriptionId++;
    subscriptions[channel] = subscriptionId;
    receiptToString[receiptId] = "Successfully subscribed to channel: " + channel;

    // יצירת פריים SUBSCRIBE
    string frame = "SUBSCRIBE\ndestination:" + channel +
                   "\nid:" + std::to_string(subscriptionId) +
                   "\nreceipt:" + std::to_string(receiptId) +
                   "\n\n\0";
    receiptId++;

    if (!connectionHandler->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "Failed to send SUBSCRIBE frame for channel: " << channel << std::endl;
        return;
    }
}

// ===============================================================================================================================
// יציאה מערוץ
void StompProtocol::processExit(const string &input)
{
    boost::shared_lock<boost::shared_mutex> readLock(isConnectedMutex); // נעילה לקריאה בלבד

    if (isConnected == false)
    {
        std::cerr << "Please connect first." << std::endl;
        return;
    }

    boost::unique_lock<boost::shared_mutex> writeLock(receiptToStringMutex); // נעילה לכתיבה

    string sanitizedInput = trim(input); // ניקוי רווחים
    if (sanitizedInput.empty())
    {
        std::cerr << "Missing the argument. Expected <channel>." << std::endl;
        return;
    }
    std::istringstream iss(input);
    string channel;
    std::getline(iss, channel, ' ');
    channel = trim(channel); // ניקוי רווחים אם יש

    if (channel.empty())
    {
        std::cerr << "Missing the argument. Expected <channel>." << std::endl;
        return;
    }
    if (iss.rdbuf()->in_avail() > 0)
    { // בדיקה אם נותרו נתונים נוספים בזרם
        std::cerr << "Illegal input length. Expected one argument." << std::endl;
        return;
    }
    if (subscriptions.find(channel) == subscriptions.end())
    {
        std::cerr << "Channel not found: " + channel << std::endl;
    }

    int subscriptionId = subscriptions[channel];
    subscriptions.erase(channel);

    // יצירת פריים UNSUBSCRIBE
    std::string frame = "UNSUBSCRIBE\nid:" + std::to_string(subscriptionId) +
                        "\nreceipt:" + std::to_string(receiptId) + "\n\n\0";
    if (!connectionHandler->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "Failed to send UNSUBSCRIBE frame for channel: " << channel << std::endl;
        return;
    }

    // עדכון המפות
    receiptToString[receiptId] = "Successfully unsubscribed from channel: " + channel;
    receiptId++;
}

// ===============================================================================================================================

// שליחת אירוע (REPORT)
void StompProtocol::processReport(const string &input)
{
    boost::shared_lock<boost::shared_mutex> lock(isConnectedMutex); // נועל לקריאה בלבד
    if (!isConnected)
    {
        std::cerr << "Please connect first." << std::endl;
        return;
    }

    std::string sanitizedInput = trim(input);
    if (sanitizedInput.empty())
    {
        std::cerr << "Missing arguments. Usage: report <file>" << std::endl;
        return;
    }

    string filePath = trim(sanitizedInput);

    if (filePath.empty())
    {
        std::cerr << "File path is empty or invalid." << std::endl;
        return;
    }

    // קריאת קובץ האירועים
    names_and_events parsedData = parseEventsFile(filePath);
    const std::string &channelName = parsedData.channel_name; // הערוץ נלקח מהקובץ
    const std::vector<Event> &events = parsedData.events;

    if (channelName.empty())
    {
        std::cerr << "Channel name is missing in the events file." << std::endl;
        return;
    }

    if (events.empty())
    {
        std::cerr << "The events file is empty or invalid." << std::endl;
        return;
    }

    // שליחת כל אירוע כ-FRAME SEND
    for (const Event &event : events)
    {
        std::ostringstream bodyStream;

        // בניית גוף ההודעה
        bodyStream << "user:" << getCurrentUsername() << "\n"
                   << "city:" << event.get_city() << "\n"
                   << "event name:" << event.get_name() << "\n"
                   << "date time:" << event.get_date_time() << "\n"
                   << "general information:\n";

        for (const auto &pair : event.get_general_information())
        {
            bodyStream << pair.first << ":" << pair.second << "\n";
        }

        bodyStream << "description:\n"
                   << event.get_description() << "\n";

        // יצירת ה-SEND frame
        string frame = "SEND\ndestination:" + channelName + "\n\n" + bodyStream.str() + "\0";

        if (!connectionHandler->sendFrameAscii(frame, '\0'))
        {
            std::cerr << "Failed to send frame for event: " << event.get_name() << std::endl;
        }
    }
}

// ===============================================================================================================================
void StompProtocol::processSummary(const string &input)
{

    boost::shared_lock<boost::shared_mutex> connectLock(isConnectedMutex); // נועל לקריאה בלבד
    if (!isConnected)
    {
        std::cerr << "[Error] Not connected. Please connect first." << std::endl;
        return;
    }
    boost::shared_lock<boost::shared_mutex> reportLock(reportFromOtherUsersMutex); // נועל לקריאה

    std::string sanitizedInput = trim(input);
    if (sanitizedInput.empty())
    {
        std::cerr << "[Error] Missing arguments. Usage: summary <channel_name> <user> <file>" << std::endl;
        return;
    }

    std::istringstream iss(sanitizedInput);
    std::string channel, user, fileName;

    // קריאה ופירוק הקלט
    std::getline(iss, channel, ' ');
    std::getline(iss, user, ' ');
    std::getline(iss, fileName);

    channel = trim(channel);
    user = trim(user);
    fileName = trim(fileName);

    if (channel.empty() || user.empty() || fileName.empty())
    {
        std::cerr << "[Error] Missing arguments. Usage: summary <channel_name> <user> <file>" << std::endl;
        return;
    }
    if (iss.rdbuf()->in_avail() > 0)
    {
        std::cerr << "[Error] Illegal input length. Expected three arguments: <channel> <user> <file>." << std::endl;
        return;
    }

    auto key = std::make_pair(channel, user);
    if (reportFromOtherUsers.find(key) == reportFromOtherUsers.end())
    {
        std::cerr << "[Error] No reports found for channel: " << channel << " and user: " << user << std::endl;
        return;
    }
    const auto &events = reportFromOtherUsers[key];
    int totalReports = events.size();
    int activeCount = 0, forcesArrivalCount = 0;

    std::vector<Event> sortedEvents = events;
    std::sort(sortedEvents.begin(), sortedEvents.end(), [](const Event &a, const Event &b)
              {
        if (a.get_date_time() == b.get_date_time()) {
            return a.get_name() < b.get_name();
        }
        return a.get_date_time() < b.get_date_time(); });

    for (const auto &event : events)
    {
        const auto &generalInfo = event.get_general_information();

        if (generalInfo.find("active") != generalInfo.end() && generalInfo.at("active") == "true")
        {
            activeCount++;
        }

        if (generalInfo.find("forces_arrival_at_scene") != generalInfo.end() && generalInfo.at("forces_arrival_at_scene") == "true")
        {
            forcesArrivalCount++;
        }
    }


    // פתיחת קובץ לכתיבה
    std::ofstream outputFile(fileName);
    if (!outputFile.is_open())
    {
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

    if (!outputFile)
    {
        std::cerr << "[Error] Error writing to file: " << fileName << std::endl;
        outputFile.close();
        return;
    }

    // כתיבת האירועים
    for (size_t i = 0; i < sortedEvents.size(); ++i)
    {
        const auto &event = sortedEvents[i];

        outputFile << "Report_" << i + 1 << ":\n";
        outputFile << "city: " << event.get_city() << "\n";
        outputFile << "date time: " << event.get_date_time() << "\n";
        outputFile << "event name: " << event.get_name() << "\n";
        outputFile << "summary: " << (event.get_description().size() > 27 ? event.get_description().substr(0, 27) + "..." : event.get_description()) << "\n";

        if (!outputFile)
        {
            std::cerr << "[Error] Error writing event report to file: " << fileName << std::endl;
            break;
        }
    }

    // סגירת הקובץ
    outputFile.close();
    if (!outputFile)
    {
        std::cerr << "[Error] Error finalizing the file: " << fileName << std::endl;
    }
    else
    {
        std::cout << "[Debug] Summary successfully written to " << fileName << std::endl;
    }
}

// ===============================================================================================================================
void StompProtocol::logout()
{
    boost::unique_lock<boost::shared_mutex> connectionLock(isConnectedMutex); // נעילה לכתיבה
    if (!isConnected)
    {
        std::cerr << "Please connect first." << std::endl;
        return;
    }

    // יצירת פריים DISCONNECT
    string frame = "DISCONNECT\nreceipt:" + std::to_string(receiptId) + "\n\n\0"; // לבדוק אם ליצור ככה או פריים
    if (!connectionHandler->sendFrameAscii(frame, '\0'))
    {
        std::cerr << "Failed to send DISCONNECT frame to the server." << std::endl;
        return;
    }
    boost::unique_lock<boost::shared_mutex> receiptLock(receiptToStringMutex);
    receiptToString[receiptId] = "Logedout";
    receiptId++;
    // isConnected = false;
}

// ===============================================================================================================================

// טיפול בתגובות מהשרת
void StompProtocol::handleServerResponse(const string &response)
{
    try
    {
        // הפיכת ה-STRING ל-FRAME
        Frame frame = parseFrame(response);
        // בדיקה של ה-COMMAND ושליחה לפונקציה המתאימה
        const std::string &command = frame.getCommand();
        if (command == "CONNECTED")
        {
            handleConnected(frame);
        }
        else if (command == "MESSAGE")
        {
            handleMessage(frame);
        }
        else if (command == "RECEIPT")
        {
            handleReceipt(frame);
        }
        else if (command == "ERROR")
        {
            handleError(frame);
        }
        else
        {
            std::cerr << "Unknown command from server: " << command << std::endl;
        }
    }
    catch (const std::exception &ex)
    {
        std::cerr << "Error while processing server response: " << ex.what() << std::endl;
    }
}

// ===============================================================================================================================
void StompProtocol::handleConnected(Frame connectFrame)
{
    if (connectFrame.getCommand() == "CONNECTED")
    {
        std::cout << "Logged in successfully" << std::endl;
    }
    else
    {
        std::cerr << "Unexpected frame type in handleConnected." << std::endl;
    }
}

// ===============================================================================================================================
void StompProtocol::handleMessage(Frame messageFrame)
{
    try
    {
        boost::unique_lock<boost::shared_mutex> lock(reportFromOtherUsersMutex);

        string destination = messageFrame.getHeader("destination");
        string body = messageFrame.getBody();
        Event event(body);

        // הסרת "/" אם מופיע בתחילת שם הערוץ
        if (!destination.empty() && destination[0] == '/')
        {
            destination = destination.substr(1);
        }

        string username = event.getEventOwnerUser();
        std::pair<std::string, std::string> key = std::make_pair(destination, username);

        reportFromOtherUsers[key].push_back(event);

        const auto &generalInfo = event.get_general_information();
        for (const auto &entry : generalInfo)
        {
            std::cout << "General Info Key: " << entry.first << ", Value: " << entry.second << std::endl;
        }
    }
    catch (const std::exception &e)
    {
        std::cerr << "[Error] Exception in handleMessage: " << e.what() << std::endl;
    }
    catch (...)
    {
        std::cerr << "[Error] Unknown error occurred in handleMessage." << std::endl;
    }
}


// ===============================================================================================================================
void StompProtocol::handleReceipt(Frame connectFrame)
{
    boost::shared_lock<boost::shared_mutex> lock(receiptToStringMutex);

    std::string receiptID = connectFrame.getHeader("receipt-id");

    try
    {
        int receipt = std::stoi(receiptID);

        if (receiptToString.find(receipt) != receiptToString.end())
        {
            std::string output = receiptToString[receipt];
            std::cout << output << std::endl;
            if (output == "Logedout")
            {
                if (connectionHandler)
                {
                    connectionHandler->close();
                    connectionHandler.reset();
                }
            }
        }
        else
        {
            std::cerr << "[Error] Receipt-id not found in receiptToString." << std::endl;
        }
    }
    catch (const std::exception &e)
    {
        std::cerr << "[Error] Failed to parse receipt-id: " << e.what() << std::endl;
    }
}

// ===============================================================================================================================
void StompProtocol::handleError(const Frame &errorFrame)
{
    boost::unique_lock<boost::shared_mutex> lock(isConnectedMutex);
    std::cout << "ERROR" << std::endl;

    for (const auto &header : errorFrame.getHeaders())
    {
        std::cout << header.first << ": " << header.second << std::endl;
    }

    std::cout << "\nThe message:\n-----" << std::endl;
    std::cout << errorFrame.getBody() << std::endl;
    std::cout << "-----" << std::endl;

    if (connectionHandler)
    {
        connectionHandler->close();
        connectionHandler.reset(); // שחרור הזיכרון
    }

    isConnected = false;
    terminateClient = true;
}

// ===============================================================================================================================
// פונקציות עזר
int StompProtocol::countWords(std::istringstream &iss)
{
    string word;
    int wordCount = 0;
    while (iss >> word)
    { // קרא מילה אחת בכל פעם
        wordCount++;
    }
    return wordCount;
}

// ===============================================================================================================================

string StompProtocol::getCurrentUsername() const
{
    return myUsername;
}
// ===============================================================================================================================
Frame StompProtocol::parseFrame(const string &response)
{
    // מציאת המיקום של "\n\n" שמפריד בין ה-Headers ל-Body
    size_t headersEnd = response.find("\n\n");
    if (headersEnd == std::string::npos)
    {
        std::cerr << "[Error] Invalid frame format (missing headers-body separator)." << std::endl;
        return Frame(""); // פריים ריק במקרה של שגיאה
    }

    // חלוקה ל-Headers ו-Body
    string headersPart = response.substr(0, headersEnd);
    string bodyPart = response.substr(headersEnd + 2);

    // יצירת פריים עם הפקודה (השורה הראשונה ב-Headers)
    std::istringstream headersStream(headersPart);
    string command;
    std::getline(headersStream, command);
    Frame frame(trim(command));

    // עיבוד ה-Headers
    string headerLine;
    while (std::getline(headersStream, headerLine))
    {
        size_t colonPos = headerLine.find(':');
        if (colonPos != std::string::npos)
        {
            string key = trim(headerLine.substr(0, colonPos));
            string value = trim(headerLine.substr(colonPos + 1));
            frame.addHeader(key, value);
        }
    }

    // הגדרת ה-Body
    frame.setBody(trim(bodyPart));

    return frame;
}

// ===============================================================================================================================
bool StompProtocol::TerminateClient()
{
    return terminateClient;
}
// ===============================================================================================================================
bool StompProtocol::IsConnected()
{
    boost::shared_lock<boost::shared_mutex> lock(isConnectedMutex); // נועל לקריאה בלבד
    return isConnected;
}

// ===============================================================================================================================
string StompProtocol::trim(const std::string &str)
{
    size_t first = str.find_first_not_of(" \t");
    if (first == std::string::npos)
        return ""; // המחרוזת ריקה או מכילה רק רווחים
    size_t last = str.find_last_not_of(" \t");
    return str.substr(first, (last - first + 1));
}

// ===============================================================================================================================
StompProtocol::~StompProtocol() = default;
