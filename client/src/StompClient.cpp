#include "../include/ConnectionHandler.h"
#include "StompProtocol.h"
#include <iostream>
#include <string>
#include <thread>
#include <mutex>
#include <condition_variable>

using namespace std;

std::mutex connectionMutex;
std::condition_variable connectionCV;

int main(int argc, char* argv[]) {
    cout << "Welcome to the STOMP client. Please type 'login <host:port> <username> <password>' to connect." << endl;

    shared_ptr<ConnectionHandler> CH = nullptr;
    StompProtocol stompProtocol(CH);
    bool firsConnect = false;

    // Thread for user input
    thread keyboardThread([&]() {
        string input;
        cout << " Keyboard thread started." << endl;

        while (getline(cin, input) && !stompProtocol.isshouldTerminate()) {
            stompProtocol.keyboardLoop(input);
             cout << " Keyboard got here1" << endl;
             cout << "Checking condition: connectionHandler is1 " << (CH ? "initialized" : "nullptr") << endl;
            cout << "Checking condition: firsConnect is " << (firsConnect ? "true" : "false") << endl;

            // Notify the server listener thread after login
            if (CH && !firsConnect) {
                unique_lock<mutex> lock(connectionMutex);
                firsConnect = true;
                cout << "Keyboard got here2" << endl;
                connectionCV.notify_one();
            }
        }
        cout << " Keyboard thread exiting." << endl;
    });

    // Thread for server communication
    thread serverThread([&]() {
        cout << " Server thread started1" << endl;
        unique_lock<mutex> lock(connectionMutex);
        connectionCV.wait(lock, [&]() { return CH != nullptr; });
        cout << " Server thread started2" << endl;

        string lastFrame;
        string response;

        while (CH && CH->getFrameAscii(response, '\0') && !stompProtocol.isshouldTerminate()) {
            if (response == lastFrame) {
                continue;  // דלג על מסגרת שכבר טופלה
            }
            if (response.empty()) {
                continue;
            }
            lastFrame = response;  // שמור את המסגרת הנוכחית כמסגרת האחרונה שטופלה
            stompProtocol.handleServerResponse(response);
            response.clear();  // נקה את התגובה לפני הקריאה הבאה
        }

        if (!CH) {
            cerr << " connectionHandler is null in serverListenerThread (address: " << CH.get() << ")." << endl;
        } else {
            cout << " connectionHandler is still valid in serverListenerThread (address: " << CH.get() << ")." << endl;
        }

                cout << " Server thread exiting." << endl;

    });

    keyboardThread.join();
    serverThread.join();

    return 0;
}