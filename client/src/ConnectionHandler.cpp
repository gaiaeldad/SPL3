#include "../include/ConnectionHandler.h"

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;


ConnectionHandler::ConnectionHandler(string host, short port) : host_(host), port_(port), io_service_(),
                                                                socket_(io_service_) {}

ConnectionHandler::~ConnectionHandler() {
	close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to " << host_ << ":" << port_ << std::endl;
	std::cout << " got to connect Socket open status before connect: " << socket_.is_open() << std::endl;

	try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error){
			std::cerr << "Connection failed1: " << error.message() << std::endl;
			throw boost::system::system_error(error);
		}
	}
	catch (std::exception &e) {
		std::cerr << "Connection failed2 (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
	size_t tmp = 0;
	std::cout << " got to getBytes Socket open status before : " << socket_.is_open() << std::endl;
	boost::system::error_code error;
	if (!socket_.is_open()) {/// i added this --------------------
		std::cerr << "Error: Socket is not open." << std::endl;
		return false;
	}
	if (bytesToRead == 0) { // i added this ---------------
		std::cerr << "Error: Attempting to read 0 bytes." << std::endl;
		return false;
	}
	try {
		while (!error && bytesToRead > tmp) {
			tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp), error);
		}
		if (error){
			throw boost::system::system_error(error);
		}
	} catch (std::exception &e) {
		std::cerr << "recv failed1 (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
	std::cout << " got to sendBytes Socket open status before : " << socket_.is_open() << std::endl;
	int tmp = 0;
	boost::system::error_code error;
	if (!socket_.is_open()) {
		std::cerr << "Socket is not open. Cannot send data." << std::endl;
		return false;
	}
	try {
		while (!error && bytesToWrite > tmp) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
		}
		if (error){
			throw boost::system::system_error(error);
		}
	} catch (std::exception &e) {
		std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::getLine(std::string &line) {
	return getFrameAscii(line, '\n');
}

bool ConnectionHandler::sendLine(std::string &line) {
	return sendFrameAscii(line, '\n');
}


bool ConnectionHandler::getFrameAscii(std::string &frame, char delimiter) {
	char ch;
	// Stop when we encounter the null character.
	// Notice that the null character is not appended to the frame string.
	try {
		do {
			if (!getBytes(&ch, 1)) {
				return false;
			}
			if (ch != '\0'){
				frame.append(1, ch);
			}		
		} while (delimiter != ch);
	} catch (std::exception &e) {
		std::cerr << "recv failed2 (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::getFrame(Frame &frame) {//our method
    std::string rawFrame;
    if (!getFrameAscii(rawFrame, '\0')) {
        return false; // כישלון בקריאה מהסוקט
    }

    try {
        frame = Frame::parse(rawFrame); // המרת מחרוזת גולמית לאובייקט Frame
    } catch (std::exception &e) {
        std::cerr << "Frame parsing failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }

    return true;
}
/// chat gave me 
bool ConnectionHandler::sendFrameAscii(const std::string &frame, char delimiter) {
	cout<< "got to sendFrameAscii " << endl;
    std::string frameWithDelimiter = frame + delimiter; // כולל תו הסיום
    return sendBytes(frameWithDelimiter.c_str(), frameWithDelimiter.length());
}
// i changened it ----------------------------------
// what we had

// bool ConnectionHandler::sendFrameAscii(const std::string &frame, char delimiter) {
// 	bool result = sendBytes(frame.c_str(), frame.length());
// 	if (!result) {		
// 		return false;
// 	}
// 	return sendBytes(&delimiter, 1);
// }

// Close down the connection properly.
void ConnectionHandler::close() {
	try {
		socket_.close();
	} catch (...) {
		std::cout << "closing failed: connection already closed" << std::endl;
	}
}