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
#include <shared_mutex>
#include <boost/thread/shared_mutex.hpp>


using namespace std;

class StompProtocol {
private:
    // lockes
    mutable boost::shared_mutex isConnectedMutex;
    mutable boost::shared_mutex eventSummaryMapMutex;
    mutable boost::shared_mutex receiptCallbacksMutex;
     // fildes
    std::shared_ptr<ConnectionHandler>& CH;
    string username;
    bool connected;
    int nextSubscriptionId;
    int nextReceiptId;
    map<string, int> topicToSubscriptionId;
    map<int, std::string> receiptCallbacks;
    map<std::string, std::map<string, vector<EmergencyEvent>>> eventSummaryMap;
    bool shouldTerminate;

    // keboard fanctions

private:
    //keybordThred
    void handleLogin(const string& hostPort, const string& username, const string& password);
    void handleJoin(const string& topic);
    void handleExit(const string& topic);
    void handleReport(const string& file);
    void handleLogout();
    void createSummary(const string& channel_name, const string& user, const string& file);

    //serverReaderThred
    void handleReceipt(Frame connectFrame);
    void handleMessage(Frame messageFrame);
    void handleError(const Frame &errorFrame);

public:
    StompProtocol(); // Default constructor
    StompProtocol(std::shared_ptr<ConnectionHandler>& handler);
    // Rule of 3
    StompProtocol(const StompProtocol &) = delete;             // Copy Constructor
    StompProtocol &operator=(const StompProtocol &) = delete;  // Assignment Operator
    ~StompProtocol();

    void keyboardLoop(const string& input);
    void handleServerResponse(const string &response);
    void handleFrame(const Frame& response);

    bool isshouldTerminate();
    void setShouldTerminate(bool value);
    

    vector<string> splitString(const string& str, char delimiter);
    string getUsername() const;
    bool IsConnected();

};

#endif // STOMPCLIENT_H
