#ifndef STOMPCLIENT_H
#define STOMPCLIENT_H

#include <iostream>
#include <thread>
#include <mutex>
#include <map>
#include <vector>
#include <string>
#include <memory>
#include "ConnectionHandler.h"
#include "Frame.h"
#include "EmergencyEvent.h"

using namespace std;

extern std::mutex connectionMutex;
extern std::condition_variable connectionCV;
extern bool connectionInitialized;


class StompProtocol {
private:
    std::shared_ptr<ConnectionHandler> CH;
    string username;
    bool connected;
    int nextSubscriptionId;
    int nextReceiptId;
    int receiptDisconnect;
    map<string, int> topicToSubscriptionId;
    map<int, bool> gotReceipt;
    mutex gotReceiptMutex;
    map<int, std::string> receiptCallbacks;
    mutex receiptCallbacksMutex;
    map<std::string, std::map<string, vector<EmergencyEvent>>> eventSummaryMap;
    mutex eventSummaryMapMutex;
    thread readThread;
    thread keyboardThread;
    bool shouldTerminate;
    bool isRunning;

public:
    StompProtocol(); // Default constructor
    StompProtocol(std::shared_ptr<ConnectionHandler> &handler);
    // Rule of 3
    StompProtocol(const StompProtocol& SP);
    StompProtocol& operator=(const StompProtocol&);
    ~StompProtocol();
    
    bool isshouldTerminate();
    void setShouldTerminate(bool value);
    void start();
    void stop();
    void keyboardLoop(const string& input);
    void handleFrame(const Frame& response);

private:
    void sendFrame(const Frame& frame);
    Frame handleLogin(const string& hostPort, const string& username, const string& password);
    Frame handleJoin(const string& topic);
    Frame handleExit(const string& topic);
    Frame handleReport(const string& file);
    Frame handleLogout();
    void createSummary(const string& channel_name, const string& user, const string& file);
    bool isReceiptValid(const Frame& frame, int receiptId);
    void readLoop();
    vector<string> splitString(const string& str, char delimiter);
};

#endif // STOMPCLIENT_H
