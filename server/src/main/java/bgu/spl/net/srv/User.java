package bgu.spl.net.srv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User<T> {
    private final String username;
    private final String password; // User's password
    private boolean isLoggedIn; // Tracks whether the user is logged in
    //changed this now from map 
    private final ConcurrentHashMap<Integer, String> subscriptions; // Subscription ID to Channel mapping
    private ConnectionHandler <T> connectionHandler;
    private int connectionID;


    // Constructor for creating a user with a username and password
    public User( String username, String password,ConnectionHandler <T> connectionHandler, int connectionID) {
        this.username = username;
        this.password = password;
        this.isLoggedIn = false; // Default to not logged in
        this.subscriptions = new ConcurrentHashMap<>();
        this.connectionHandler = connectionHandler;
        this.connectionID = connectionID;
    }

    // Get the username
    public String getUsername() {
        return username;
    }

    // Get the user's password
    public String getPassword() {
        return password;
    }


    // Check if the user is logged in
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    // Set the user's login state
    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    // Get the user's subscriptions
    public Map<Integer, String> getSubscriptions() {
        return subscriptions;
    }

    // Add a subscription
    public void subscribe(int subscriptionId, String channel) {
        subscriptions.put(subscriptionId, channel);
    }

    // Remove a subscription
    public void unsubscribe(int subscriptionId) {
        subscriptions.remove(subscriptionId);
    }

    // Get the channel associated with a subscription ID
    public String getChannelBySubscriptionId(int subscriptionId) {
        return subscriptions.get(subscriptionId);
    }
    public ConnectionHandler<T> getConnectionHandler() {
        return this.connectionHandler;
     }
     public void setConnectionHandler(ConnectionHandler<T> handler) {
        this.connectionHandler = handler;
     }
     public int getConnectionID() {
        return this.connectionID;
     }
     public void setConnectionID(int connectionID) {
        this.connectionID = connectionID;
     }
     public void setisLoggedIn(boolean isConnected) {
        this.isLoggedIn = isConnected;
     }




}