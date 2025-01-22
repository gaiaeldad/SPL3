package bgu.spl.net.srv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    private final String username;
    private final Map<Integer, String> subscriptions; // Subscription ID to Channel mapping

    public User(String username) {
        this.username = username;
        this.subscriptions = new ConcurrentHashMap<>();
    }

    public String getUsername() {
        return username;
    }

    public Map<Integer, String> getChannels() {
        return subscriptions;
    }

    public void subscribe(int subscriptionId, String channel) {
        subscriptions.put(subscriptionId, channel);
    }

    public void unsubscribe(int subscriptionId) {
        subscriptions.remove(subscriptionId);
    }
}
