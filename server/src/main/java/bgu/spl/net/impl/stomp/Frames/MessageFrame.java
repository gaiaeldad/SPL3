package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class MessageFrame extends Frame {
    private final String destination;
    private final String messageId;
    private final String subscription;
    private final String contentType;
    private final int contentLength;

    public MessageFrame(Map<String, String> headers, String body, Connections<String> connections, int connectionId) {
        super(headers, body, connections, connectionId);
        this.destination = headers.getOrDefault("destination", "");
        this.messageId = headers.getOrDefault("message-id", "");
        this.subscription = headers.getOrDefault("subscription", "");
        this.contentType = headers.get("content-type");
        this.contentLength = headers.containsKey("content-length") 
            ? Integer.parseInt(headers.get("content-length")) 
            : body.length();

        if (destination.isEmpty() || messageId.isEmpty() || subscription.isEmpty()) {
            throw new IllegalArgumentException("Missing required headers for MESSAGE frame.");
        }
    }

    @Override
    public void process() {
        connections.send(destination, this.toString());
    }

    @Override
    public String getCommand() {
        return "MESSAGE";
    }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder(getCommand()).append("\n");
        msg.append("destination:").append(destination).append("\n");
        msg.append("message-id:").append(messageId).append("\n");
        msg.append("subscription:").append(subscription).append("\n");
        if (contentType != null) {
            msg.append("content-type:").append(contentType).append("\n");
        }
        msg.append("content-length:").append(contentLength).append("\n");
        msg.append("\n").append(getBody());
        return msg.toString();
    }
}
