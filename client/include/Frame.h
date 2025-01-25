#pragma once

#include <string>
#include <unordered_map>
#include <vector>

using std::string;
using std::unordered_map;
using std::vector;

class Frame {
private:
    string command;
    unordered_map<string, string> headers;
    string body;

public:
    Frame(const string& command, unordered_map<string, string> headers, string body);
    Frame(const string& command);
    Frame();

    string getCommand() const;
    void setCommand(const string& command);

    void addHeader(const string& key, const string& value);
    string getHeader(const string& key) const;
    unordered_map<string, string> getHeaders() const;

    string getBody() const;
    void setBody(const string& body);
    
    string toString() const;

    ~Frame();
};
