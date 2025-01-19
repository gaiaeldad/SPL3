package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConnectionsImpl<T> implements Connections<T> {

    private final ConcurrentMap<Integer, ConnectionHandler<T>> activeConnections; // Map of connectionId ->
                                                                                  // ConnectionHandler
    private final ConcurrentMap<String, CopyOnWriteArraySet<Integer>> channels; // Map of channel -> Set of
                                                                                // connectionIds

    public ConnectionsImpl() {
        activeConnections = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
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
        activeConnections.remove(connectionId); // Remove the client from active connections
        // Remove the client from all channels
        for (CopyOnWriteArraySet<Integer> subscribers : channels.values()) {
            subscribers.remove(connectionId);
        }
    }

    // Helper method to add a connection
    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        activeConnections.put(connectionId, handler);
    }

    // Helper method to subscribe a connection to a channel
    public void subscribe(String channel, int connectionId) {
        channels.putIfAbsent(channel, new CopyOnWriteArraySet<>());
        channels.get(channel).add(connectionId);
    }

    // Helper method to unsubscribe a connection from a channel
    public void unsubscribe(String channel, int connectionId) {
        CopyOnWriteArraySet<Integer> subscribers = channels.get(channel);
        if (subscribers != null) {
            subscribers.remove(connectionId);
        }
    }
}
