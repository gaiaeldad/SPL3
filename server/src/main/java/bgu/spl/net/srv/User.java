package bgu.spl.net.srv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    private final String username;
    private final String password; // User's password
    private boolean isLoggedIn; // Tracks whether the user is logged in
    private final Map<Integer, String> subscriptions; // Subscription ID to Channel mapping

    // Constructor for creating a user with a username and password
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.isLoggedIn = false; // Default to not logged in
        this.subscriptions = new ConcurrentHashMap<>();
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
    

}
