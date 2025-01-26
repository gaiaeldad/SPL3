package bgu.spl.net.srv;

import java.util.LinkedList;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    void addConnection(int id,ConnectionHandler<T> handler);

    // Added methods for user management
    void login(int connectionId, String username, String password);

    boolean isLegalCredentials(String username, String password);

    boolean isUserLogedIn(String username);

    void subscribe( int subscriptionId, int connectionId,String channel);

    void unsubscribe(int subscriptionId, int connectionId);

    // Added methods for message and channel management
    boolean isChannelAndSubscribe(String channel, int connectionId);

    LinkedList<Integer> getConnectionIdsOfChannel(String channel);

    int getAndIncMsgIdCounter();

    ConnectionHandler<T> getHandler(int connectionId);

    void unsubscribeFromChannels (User user);

    boolean isDestinationLegal(String channel, int connectionId);

    void addConnectionHandler (ConnectionHandler<T> handler, int connectionId);
}
