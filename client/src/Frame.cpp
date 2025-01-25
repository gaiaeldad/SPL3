#include "Frame.h"
#include <sstream>

using std::ostringstream;

Frame::Frame(const string& command) 
    : command(command), headers(), body("") {} // עכשיו הסדר תואם ל-header

// ===============================================================================================================================
string Frame::getCommand() const {
    return command;
}

// ===============================================================================================================================
void Frame::setCommand(const string& command) {
    this->command = command;
}

// ===============================================================================================================================
void Frame::addHeader(const string& key, const string& value) {
    headers[key] = value;
}

// ===============================================================================================================================
string Frame::getHeader(const string& key) const {
    auto it = headers.find(key);
    if (it != headers.end()) {
        return it->second;
    }
    return "";
}

// ===============================================================================================================================

unordered_map<string, string> Frame::getHeaders() const {
    return headers;
}

// ===============================================================================================================================
string Frame::getBody() const {
    return body;
}

// ===============================================================================================================================
void Frame::setBody(const string& body) {
    this->body = body;
}

// ===============================================================================================================================
// Generate Frame as a formatted string
string Frame::toString() const {
    ostringstream frameBuilder;
    frameBuilder << command << "\n";
    for (const auto& header : headers) {
        frameBuilder << header.first << ":" << header.second << "\n";
    }
    // Add a blank line to separate headers from the body
    frameBuilder << "\n";
    // Append the body (if exists)
    if (!body.empty()) {
        frameBuilder << body;
    }
    // Append the null character (STOMP frame terminator)
    frameBuilder << '\0';
    return frameBuilder.str();
}
// ===============================================================================================================================
Frame::~Frame() = default;
