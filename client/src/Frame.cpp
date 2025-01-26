#include "Frame.h"
#include <sstream>
#include <stdexcept>
#include <iostream>

// שימוש ב-using namespace std כדי לפשט את הקוד
using namespace std;

Frame::Frame() : command(""), headers(), body("") {}
Frame::Frame(const string& command) : command(command), headers(), body("") {}


Frame Frame::parse(const string& rawFrame) {
    Frame frame;
    istringstream stream(rawFrame);

    // Parse command
    if (!getline(stream, frame.command) || frame.command.empty()) {
        throw runtime_error("Invalid frame: missing command");
    }

    // Parse headers
    string line;
    while (getline(stream, line) && !line.empty()) {
        size_t colonPos = line.find(':');
        if (colonPos == string::npos) {
            throw runtime_error("Invalid frame: malformed header");
        }
        string key = line.substr(0, colonPos);
        string value = line.substr(colonPos + 1);
        frame.headers[key] = value;
    }

    // Parse body
    ostringstream bodyStream;
    while (getline(stream, line)) {
        bodyStream << line << '\n';
    }
    frame.body = bodyStream.str();

    return frame;
}

string Frame::toString() const {
    ostringstream ss;
    // Append command
    ss << command << "\n";
    // Append headers
    for (const auto& header : headers) {
        ss << header.first << ":" << header.second << "\n";
    }
    // Append empty line and body
    ss << "\n";
    if (!body.empty()) {
        ss << body;
    }
    // Append null terminator
    ss << '\0';
    return ss.str();
}

string Frame::getCommand() const {
    return command;
}
string Frame::getHeader(const string& key) const {
    auto it = headers.find(key);
    if (it != headers.end()) {
        return it->second;
    }
    return "";
}

map<string, string> Frame::getHeaders() const {
    return headers;
}
string Frame::getBody() const {
    return body;
}

void Frame::addHeader(const string& key, const string& value) {
    headers[key] = value;
}

void Frame::setBody(const string& body) {
    this->body = body;
}

Frame Frame::parseFrame(const string &response){
    // מציאת המיקום של "\n\n" שמפריד בין ה-Headers ל-Body
    size_t headersEnd = response.find("\n\n");
    if (headersEnd == std::string::npos)
    {
        cout << "[Error] Invalid frame format (missing headers-body separator)." << endl;
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

string Frame::trim(const std::string &str)
{
    size_t first = str.find_first_not_of(" \t");
    if (first == std::string::npos)
        return ""; // המחרוזת ריקה או מכילה רק רווחים
    size_t last = str.find_last_not_of(" \t");
    return str.substr(first, (last - first + 1));
}