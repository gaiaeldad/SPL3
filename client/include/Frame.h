#ifndef FRAME_H
#define FRAME_H

#include <string>
#include <map>
using namespace std;

class Frame {
public:
    string command;   // Command of the frame (e.g., CONNECT, SUBSCRIBE, etc.)
    map<string, string> headers; // Headers as key-value pairs
    string body;                        // Body of the frame

    Frame();
    // Parses a raw STOMP frame string into a Frame object
    Frame(const string& command);
    //for empty frame
    static Frame parse(const string& rawFrame);
    // Converts a Frame object into a raw STOMP frame string
    string toString() const;
    //getter methods we added
    string getCommand() const;
    string getHeader(const string& key) const;
    map<string, string> getHeaders() const;
    string getBody() const;
    void addHeader(const string& key, const string& value);
    void setBody(const string& body);
    ///
    static Frame parseFrame(const string& rawFrame);
    static string trim(const string &str);


};

#endif // FRAME_H