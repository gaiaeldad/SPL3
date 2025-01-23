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
        if (subscribers != null) {
            for (int connectionId : subscribers) {
                send(connectionId, msg); // Send the message to each subscriber
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        // Remove the user from logged-in users
        String username = getUsernameByConnectionId(connectionId);
        if (username != null) {
            loggedInUsers.remove(username);
        }
    
        // Remove the connection handler
        ConnectionHandler<T> handler = connectionHandlers.remove(connectionId);
        if (handler != null) {
            User user = handler.getUser();
            if (user != null) {
                // Unsubscribe the user from all channels
                unsubscribeFromChannels(user);
    
                // Mark the user as disconnected
                user.setLoggedIn(false);
            }
        }
    
        // Remove the connection ID from all channels
        for (CopyOnWriteArraySet<Integer> subscribers : channels.values()) {
            subscribers.remove(connectionId);
        }
    }
    

    @Override//good 
    //changed this for the ides also chnages the interface 
    public void addConnection(int id,ConnectionHandler<T> handler) {
        connectionHandlers.put(id, handler);
    }

    // Helper method for user login//good 
    public void login(int connectionID, String userName, String password) {
        ConnectionHandler<T> newUserHandler = (ConnectionHandler)this.connectionHandlers.get(connectionID);
        User user;
        if (!this.alltimeUsers.containsKey(userName)) {//doesnt excits- open new user 
           user = new User( userName, password, (ConnectionHandler)this.connectionHandlers.get(connectionID),connectionID);
           this.alltimeUsers.put(userName, user);
        } else {//user allready excits 
           user = (User)this.alltimeUsers.get(userName);
           user.setisLoggedIn(true);
           user.setConnectionID(connectionID);
           user.setConnectionHandler(newUserHandler);
        }

        loggedInUsers.put(userName, connectionID);
        newUserHandler.setUser(user);
     }

    public void logout(String username) {
        User <T> user = alltimeUsers.get(username);
        if (user != null) {
            user.setLoggedIn(false);
            //this.unsubscribeFromAllChannels(user);///need to add 
            user.setConnectionHandler((ConnectionHandler)null);
            user.setConnectionID(-1);
            loggedInUsers.remove(username);
        }
    }

    // Validate login credentials//good 
    public boolean isLegalCredentials(String username, String password) {
        return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
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
    //good 
    public void subscribe(String channel, int subscriptionId, int connectionId) {
        Map<Integer, String> channelsforUser = (this.connectionHandlers.get(connectionId)).getUser().getSubscriptions();
        channelsforUser.put(subscriptionId, channel);
        if (!this.channels.containsKey(channel)) {
            this.channels.put(channel, new CopyOnWriteArraySet());
        }

        (this.channels.get(channel)).add(connectionId);
    }

    // Helper method to unsubscribe a connection from a channel//good 
    public void unsubscribe(int subscriptionId, int connectionId) {
        Map<Integer, String> channelsforUser = (connectionHandlers.get(connectionId)).getUser().getSubscriptions();
        String channel = channelsforUser.get(subscriptionId);
        (channels.get(channel)).remove(connectionId);
        channelsforUser.remove(subscriptionId);
     }



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

@Override//goog 
    public void unsubscribeFromChannels(User user){
            // Iterate over the user's subscriptions
            for (Map.Entry<Integer, String> subscription : ((Map<Integer, String>) user.getSubscriptions()).entrySet()) {
                String channelName = subscription.getValue();
                int connectionId = loggedInUsers.get(user.getUsername());
        
                // Remove the user's connectionId from the channel's subscriber list
                if (channels.containsKey(channelName)) {
                    channels.get(channelName).remove(connectionId);
                }
            }
        
            // Clear the user's subscription map
            user.getSubscriptions().clear();
        
            System.out.println("User " + user.getUsername() + " unsubscribed from all channels.");
        }
        
    }

}
