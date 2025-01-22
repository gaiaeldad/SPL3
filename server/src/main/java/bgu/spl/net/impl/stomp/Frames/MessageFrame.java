package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;

import java.util.HashMap;
import java.util.Map;


public class MessageFrame extends Frame {
    public MessageFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
        super(headers, body, connections, connectionId);
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
