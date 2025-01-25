#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include <iostream>
#include <string>
#include <thread>
#include <mutex>
#include <condition_variable> // משתנה תנאי

// משתנים גלובליים
std::mutex connectionMutex;
std::condition_variable connectionCV;

int main() {

    // יצירת ConnectionHandler
    std::shared_ptr<ConnectionHandler> connectionHandler = nullptr;

    bool firsConnect = false;

    // יצירת אובייקט StompProtocol
    StompProtocol stompProtocol(connectionHandler);

    // תהליך קלט המשתמש
std::thread userInputThread([&]() {
    std::string input;

    while (std::getline(std::cin, input) && !stompProtocol.TerminateClient()) {

        // עיבוד הקלט
        stompProtocol.processKeyboard(input);

        // בדיקה אם החיבור פעיל
        if (connectionHandler && !firsConnect) {
            std::unique_lock<std::mutex> lock(connectionMutex);
            firsConnect = true;
            connectionCV.notify_one();
        }
    }
});

    // תהליך האזנה לשרת
    std::thread serverListenerThread([&]() {

        std::unique_lock<std::mutex> lock(connectionMutex);
        connectionCV.wait(lock, [&]() { return connectionHandler != nullptr; }); // ממתין לחיבור

        std::string lastFrame;
        std::string response;

        while (connectionHandler && connectionHandler->getFrameAscii(response, '\0') && !stompProtocol.TerminateClient()) {
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

        if (!connectionHandler) {
            std::cerr << "[Error] connectionHandler is null in serverListenerThread (address: " << connectionHandler.get() << ")." << std::endl;
        } else {
            std::cout << "[Debug] connectionHandler is still valid in serverListenerThread (address: " << connectionHandler.get() << ")." << std::endl;
        }

    });

    // הצטרפות לתהליכים
    userInputThread.join();
    serverListenerThread.join();

    return 0;
}
