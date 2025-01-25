#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/Frame.h"
#include "../include/event.h"
#include <string>
#include <boost/thread/shared_mutex.hpp>
#include <unordered_map>
#include <fstream>
#include <unordered_set>

using std::pair;
using std::string;

// Custom hash for std::pair
struct pair_hash
{
    template <class T1, class T2>
    std::size_t operator()(const std::pair<T1, T2> &pair) const
    {
        auto hash1 = std::hash<T1>{}(pair.first);
        auto hash2 = std::hash<T2>{}(pair.second);
        return hash1 ^ (hash2 << 1); // Combine the two hashes
    }
};

class StompProtocol
{
private:
    // לפי הסדר שמופיע ב-CPP:
    std::shared_ptr<ConnectionHandler>& connectionHandler; // הפניה למצביע משותף
    mutable boost::shared_mutex isConnectedMutex;
    mutable boost::shared_mutex reportFromOtherUsersMutex;
    mutable boost::shared_mutex receiptToStringMutex;

    bool isConnected;
    string myUsername;
    unordered_map<string, int> subscriptions;                                           // מפה לsubscriptionId
    unordered_map<pair<string, string>, vector<Event>, pair_hash> reportFromOtherUsers; // לsummary
    unordered_map<int, string> receiptToString;                                         // לפי קבלה אומר מה להדפיס למסך
    int subscriptionId = 1;                                                             // מזהה ייחודי למנויים
    int receiptId = 1;                                                                  // מזהה ייחודי לקבלות
    bool terminateClient;

    // from console
    void processLogin(const string &input);
    void processJoin(const string &input);
    void processExit(const string &input);
    void processReport(const string &input);
    void processSummary(const string &input);
    void logout();
    string trim(const std::string &str);

    // from server
    Frame parseFrame(const std::string &response);
    void handleConnected(Frame frame);
    void handleMessage(Frame frame);
    void handleReceipt(Frame frame);
    void handleError(const Frame &errorFrame);

public:
    // שינוי הבנאי לקבל הפניה למצביע משותף
    StompProtocol(std::shared_ptr<ConnectionHandler>& handler);

    StompProtocol(const StompProtocol &) = delete;             // Copy Constructor
    StompProtocol &operator=(const StompProtocol &) = delete;  // Assignment Operator

    void handleServerResponse(const string &response);
    void processKeyboard(string input);

    int countWords(std::istringstream &iss);
    string getCurrentUsername() const;
    bool TerminateClient();
    bool IsConnected();

    ~StompProtocol();
};
