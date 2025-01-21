package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class MessageFrame extends Frame {
    public MessageFrame(String destination, String messageId, String subscription, String body,
            Connections<String> connections, int connectionId) {
        super(
                Map.of(
                        "destination", destination,
                        "message-id", messageId,
                        "subscription", subscription),
                body,
                connections,
                connectionId);
    }

    @Override
    public void process() {
        // Processing logic is not needed for this server-sent frame.
    }

    @Override
    public String getCommand() {
        return "MESSAGE";
    }
}
