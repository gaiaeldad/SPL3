
#include "StompProtocol.h"
#include <iostream>
#include <string>
#include <thread>
#include <mutex>
#include <condition_variable>

using namespace std;

int main(int argc, char* argv[]) {
    cout << "Welcome to the STOMP client. Please type 'login <host:port> <username> <password>' to connect." << endl;

    shared_ptr<ConnectionHandler> connectionHandler = nullptr;
    StompProtocol stompProtocol(connectionHandler);

    // Thread for user input
    thread keyboardThread([&]() {
        string input;
        cout << "[Debug] Keyboard thread started." << endl;

        while (getline(cin, input) && !stompProtocol.isshouldTerminate()) {
            stompProtocol.keyboardLoop(input);

            // Notify the server listener thread after login
            if (!connectionInitialized && connectionHandler != nullptr) {
                unique_lock<mutex> lock(connectionMutex);
                connectionInitialized = true;
                connectionCV.notify_one();
            }
        }

        cout << "[Debug] Keyboard thread exiting." << endl;
    });

    // Thread for server communication
    thread serverThread([&]() {
        {
            unique_lock<mutex> lock(connectionMutex);
            connectionCV.wait(lock, [&]() { return connectionInitialized; });
        }

        cout << "[Debug] Server thread started." << endl;

        while (!stompProtocol.isshouldTerminate()) {
            Frame response;
            if (connectionHandler != nullptr && connectionHandler->getFrame(response)) {
                stompProtocol.handleFrame(response);
            } else {
                cerr << "[Error] Failed to receive response or connection is null." << endl;
                stompProtocol.setShouldTerminate(true);
            }
        }

        cout << "[Debug] Server thread exiting." << endl;
    });

    keyboardThread.join();
    serverThread.join();

    return 0;
}