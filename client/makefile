CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lpthread -lboost_thread

all: StompEMIClient

StompEMIClient: bin/ConnectionHandler.o bin/StompProtocol.o bin/event.o bin/Frame.o bin/StompClient.o
	g++ -o bin/StompEMIClient bin/ConnectionHandler.o bin/StompProtocol.o bin/event.o bin/Frame.o bin/StompClient.o $(LDFLAGS)

bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/StompProtocol.o: src/StompProtocol.cpp
	g++ $(CFLAGS) -o bin/StompProtocol.o src/StompProtocol.cpp

bin/event.o: src/event.cpp
	g++ $(CFLAGS) -o bin/event.o src/event.cpp

bin/Frame.o: src/Frame.cpp
	g++ $(CFLAGS) -o bin/Frame.o src/Frame.cpp

bin/StompClient.o: src/StompClient.cpp
	g++ $(CFLAGS) -o bin/StompClient.o src/StompClient.cpp

.PHONY: clean
clean:
	rm -f bin/*
