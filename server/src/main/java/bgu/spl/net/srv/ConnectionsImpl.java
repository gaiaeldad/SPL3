package bgu.spl.net.srv;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {

    private final ConcurrentMap<Integer, ConnectionHandler<T>> connectionHandlers; // Map of connectionId -> ConnectionHandler
    private final ConcurrentMap<String, CopyOnWriteArraySet<Integer>> channels; // Map of channel -> Set of connectionIds
    private final ConcurrentMap<String, String> userCredentials; // Map of username -> password
    private final ConcurrentMap<String, Integer> loggedInUsers; // Map of username -> connectionId
    private final ConcurrentMap<String, User> alltimeUsers; //  map of username -> User
    private int messageIdCounter = 0; // Counter for unique message IDs
   

    public ConnectionsImpl() {
        connectionHandlers = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
        userCredentials = new ConcurrentHashMap<>();
        loggedInUsers = new ConcurrentHashMap<>();
        alltimeUsers = new  ConcurrentHashMap<>();
    }

    @Override
    //sends to a specific client 
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = connectionHandlers.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false; 
    }

    @Override
    //sends to clients subscribed to the channel 
    public void send(String channel, T msg) {
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        System.out.println("[Debug] Subscribers for channel " + channel + ": " + channels.get(channel));

        if (subscribers != null) {
            synchronized (subscribers){
            for (int connectionId : subscribers) {
                send(connectionId, msg); // Send the message to each subscriber
            }
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        // Remove the user from logged-in users
        System.out.println("[Debug] Disconnecting connectionId: " + connectionId);
        synchronized (loggedInUsers) {
        String username = getUsernameByConnectionId(connectionId);
        if (username != null) {
            loggedInUsers.remove(username);
            System.out.println("[Debug] Removed user " + username + " from loggedInUsers");
        }
    }
    
        // Remove the connection handler
        ConnectionHandler<T> handler = null;
        synchronized (connectionHandlers) {
         handler = connectionHandlers.remove(connectionId);
        }
        if (handler != null) {
            User user = handler.getUser();
            if (user != null) {
                // Unsubscribe the user from all channels
                unsubscribeFromChannels(user);
    
                // Mark the user as disconnected
                user.setLoggedIn(false);
            }
            System.out.println("[Debug] Disconnect cleanup complete for connectionId: " + connectionId);
        }
    
        // Remove the connection ID from all channels
        synchronized (channels) {
        for (CopyOnWriteArraySet<Integer> subscribers : channels.values()) {
            subscribers.remove(connectionId);
        }
    }
    }

    

    @Override//good 
    //changed this for the ides also chnages the interface 
    public void addConnection(int id,ConnectionHandler<T> handler) {
        connectionHandlers.put(id, handler);
        System.out.println("[Debug] Added connectionId: " + id + " with handler: " + handler);
    }

    // Helper method for user login//good 
    public void login(int connectionID, String userName, String password) {
        // Retrieve the connection handler (ConcurrentHashMap is thread-safe for this operation)
        ConnectionHandler<T> newUserHandler = connectionHandlers.get(connectionID);
        if (newUserHandler == null) {
            System.out.println("[Error] Cannot log in: ConnectionHandler not found for connectionId: " + connectionID);
            return; // Prevent further execution
        }
        System.out.println("[Debug] Found handler for connectionId: " + connectionID);

        User user;
        synchronized (alltimeUsers) {
            if (!alltimeUsers.containsKey(userName)) {
                // New user: Create and store in alltimeUsers
                user = new User(userName, password, newUserHandler, connectionID);
                alltimeUsers.put(userName, user);
            } else {
                // Existing user: Update its state
                user = alltimeUsers.get(userName);
                synchronized (user) { // Lock only the specific user object for updates
                    user.setisLoggedIn(true);
                    user.setConnectionID(connectionID);
                    user.setConnectionHandler(newUserHandler);
                }
            }
        }
    
        // Update loggedInUsers (ConcurrentHashMap handles thread safety for put operations)
        loggedInUsers.put(userName, connectionID);
    
        // Associate the user with the connection handler, if present
        if (newUserHandler != null) {
            newUserHandler.setUser(user);
        }
    }
    

    public void logout(String username) {
        User <T> user = alltimeUsers.get(username);
        if (user != null) {
            user.setLoggedIn(false);
            this.unsubscribeFromChannels(user);
            user.setConnectionHandler((ConnectionHandler)null);
            user.setConnectionID(-1);
            loggedInUsers.remove(username);
        }
    }

    // Validate login credentials//good 
    public boolean isLegalCredentials(String username, String password) {
        return !this.alltimeUsers.containsKey(username) || ((User)this.alltimeUsers.get(username)).getPassword().equals(password);
    }

    // Check if a user is already logged in//good 
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
    
    public void subscribe( int subscriptionId, int connectionId, String channel) {
        Map<Integer, String> subscriptions = connectionHandlers.get(connectionId).getUser().getSubscriptions();
        subscriptions.put(subscriptionId, channel);
        System.out.println("[Debug] Subscribed connectionId " + connectionId + " to channel " + channel);
        

    
        // Initialize the channel if it doesn't exist
        channels.computeIfAbsent(channel, key -> new CopyOnWriteArraySet<>());
        channels.get(channel).add(connectionId);
        System.out.println("[Debug] Current subscribers for channel " + channel + ": " + channels.get(channel));

    
        System.out.println("[Debug] Subscribed connectionId " + connectionId + " to channel " + channel);
    }
    
    

    

    // Helper method to unsubscribe a connection from a channel//good 
    public void unsubscribe(int subscriptionId, int connectionId) {
        System.out.println("[Debug] Unsubscribing connectionId: " + connectionId + ", subscriptionId: " + subscriptionId);

        Map<Integer, String> channelsforUser = (connectionHandlers.get(connectionId)).getUser().getSubscriptions();
        if (channelsforUser == null) {
            System.out.println("[Error] Subscriptions are null for connectionId: " + connectionId);
            return; // Exit if there are no subscriptions for the user
        }
        String channel = channelsforUser.get(subscriptionId);
        if (channel == null) {
            System.out.println("[Error] No channel found for subscriptionId: " + subscriptionId);
            return; // Exit if the subscription ID is invalid
        }
        if (!channels.containsKey(channel)) {
            System.out.println("[Error] Channel not found: " + channel);
            return; // Exit if the channel does not exist
            
        }
        synchronized (channels.get(channel)) {
            channels.get(channel).remove(connectionId);
            System.out.println("[Debug] Removed connectionId: " + connectionId + " from channel: " + channel);
        }
            channelsforUser.remove(subscriptionId);
            System.out.println("[Debug] Removed subscriptionId: " + subscriptionId + " for connectionId: " + connectionId);
     }


//good 
    public boolean isChannelAndSubscribe(String channel, int connectionId) {
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        return subscribers != null && subscribers.contains(connectionId);
    }

    // Retrieve connection IDs of a specific channel//good 
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
        return connectionHandlers.get(connectionId);
    }

    @Override
    public void unsubscribeFromChannels(User user) {
        Map<Integer, String> subscriptions = user.getSubscriptions();
    
        for (Map.Entry<Integer, String> entry : subscriptions.entrySet()) {
            String channel = entry.getValue();
            if (channel != null && channels.containsKey(channel)) {
                channels.get(channel).remove(user.getConnectionID());
            } else {
                System.out.println("[Warning] Channel " + channel + " not found.");
            }
        }
    
        subscriptions.clear();
        System.out.println("[Debug] Cleared subscriptions for user: " + user.getUsername());
    }
    @Override
public boolean isDestinationLegal(String channel, int connectionId) {
    System.out.println("[Debug] Validating channel: " + channel + " for connectionId: " + connectionId);

    // Check if the channel exists
    if (!channels.containsKey(channel)) {
        System.out.println("[Debug] Channel not found: " + channel);
        return false;
    }

    // Check if the user is subscribed
    ConnectionHandler<T> handler = connectionHandlers.get(connectionId);
    if (handler == null) {
        System.out.println("[Error] ConnectionHandler not found for connectionId: " + connectionId);
        return false;
    }
    User user = handler.getUser();
    if (user == null) {
        System.out.println("[Error] User not found for connectionId: " + connectionId);
        return false;
    }

    Map<Integer, String> subscriptions = user.getSubscriptions();
    boolean isSubscribed = subscriptions.containsValue(channel);

    System.out.println("[Debug] Channel validation: channel=" + channel +
                       ", connectionId=" + connectionId + ", isSubscribed=" + isSubscribed);

    return isSubscribed;
}

    
    
    
    public void debugActiveConnections() {
        System.out.println("[Debug] Active connections: " + connectionHandlers.keySet());
    }
    public void addConnectionHandler(ConnectionHandler<T> handler, int connectionId) {
        this.connectionHandlers.put(connectionId, handler);
     }
    
}
    


