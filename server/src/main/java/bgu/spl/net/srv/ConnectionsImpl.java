package bgu.spl.net.srv;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConnectionsImpl<T> implements Connections<T> {

    private final ConcurrentMap<Integer, ConnectionHandler<T>> activeConnections; // Map of connectionId ->
                                                                                  // ConnectionHandler
    private final ConcurrentMap<String, CopyOnWriteArraySet<Integer>> channels; // Map of channel -> Set of
                                                                                // connectionIds
    private final ConcurrentMap<String, String> userCredentials; // Map of username -> password////not sure if we need
                                                                 // thsi
    private final ConcurrentMap<String, Integer> loggedInUsers; // Map of username -> connectionId

    private final ConcurrentMap<String, User> alltimeUsers; // Persistent map of username -> User

    
    private int messageIdCounter = 0; // Counter for unique message IDs

    public ConnectionsImpl() {
        activeConnections = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
        userCredentials = new ConcurrentHashMap<>();
        loggedInUsers = new ConcurrentHashMap<>();
        alltimeUsers = new  ConcurrentHashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeConnections.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false; // Client not found
    }

    @Override
    public void send(String channel, T msg) {
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        if (subscribers != null) {
            for (int connectionId : subscribers) {
                send(connectionId, msg); // Send the message to each subscriber
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        String username = getUsernameByConnectionId(connectionId);
        if (username != null) {
            loggedInUsers.remove(username);
        }
        activeConnections.remove(connectionId); // Remove the client from active connections
        // Remove the client from all channels
        for (CopyOnWriteArraySet<Integer> subscribers : channels.values()) {
            subscribers.remove(connectionId);
        }
    }

    @Override
    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        activeConnections.put(connectionId, handler);
    }

    // Helper method for user login
    public void login(int connectionId, String username, String password) {
        User user = alltimeUsers.computeIfAbsent(username, key -> new User(username, password)); // Retrieve or create user

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (user.isLoggedIn()) {
            throw new IllegalStateException("User already logged in");
        }

        user.setLoggedIn(true); // Mark user as logged in
        loggedInUsers.put(username, connectionId); // Associate user with connection ID
        activeConnections.get(connectionId).setUser(user); // Associate user with the connection handler
    }

    public void logout(String username) {
        User user = alltimeUsers.get(username);
        if (user != null) {
            user.setLoggedIn(false);
            loggedInUsers.remove(username);
        }
    }

    // Validate login credentials
    public boolean isLegalLoginInfo(String username, String password) {
        return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
    }

    // Check if a user is already logged in
    public boolean isUserLogedIn(String username) {
        return loggedInUsers.containsKey(username);
    }

    // Get username by connectionId
    private String getUsernameByConnectionId(int connectionId) {
        return loggedInUsers.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(connectionId))
                .map(entry -> entry.getKey())
                .findFirst()
                .orElse(null);
    }

    // Helper method to subscribe a connection to a channel
    public void subscribe(String channel, int subscriptionId, int connectionId) {
        Map<Integer, String> userChannels = (this.activeConnections.get(connectionId)).getUser().getSubscriptions();
        userChannels.put(subscriptionId, channel);
        if (!this.channels.containsKey(channel)) {
            this.channels.put(channel, new CopyOnWriteArraySet());
        }

        (this.channels.get(channel)).add(connectionId);
    }

    // Helper method to unsubscribe a connection from a channel
    public void unsubscribe(String channel, int connectionId) {
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        if (subscribers != null) {
            subscribers.remove(connectionId);
        }
    }

    public boolean isChannelAndSubscribe(String channel, int connectionId) {
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        return subscribers != null && subscribers.contains(connectionId);
    }

    // Retrieve connection IDs of a specific channel
    public LinkedList<Integer> getConnectionIdsOfChannel(String channel) {
        LinkedList<Integer> connectionIds = new LinkedList<>();
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        if (subscribers != null) {
            connectionIds.addAll(subscribers);
        }
        return connectionIds;
    }

    // Increment and retrieve the message ID counter
    public synchronized int getAndIncMsgIdCounter() {
        return messageIdCounter++;
    }

    // Get the handler for a specific connection ID
    public ConnectionHandler<T> getHandler(int connectionId) {
        return activeConnections.get(connectionId);
    }

}
