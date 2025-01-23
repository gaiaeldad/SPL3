package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class ConnectedFrame extends Frame {
    ConnectedFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
        super(headers, body, connections, connectionId);
     }

    @Override
    public void process() {
        // Processing logic is usually not required for server-sent frames.
    }

    @Override
    public String getCommand() {
        return "CONNECTED";
    }
}
