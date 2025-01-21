package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class ErrorFrame extends Frame {
    public ErrorFrame(String message, String details, String receiptId, Connections<String> connections,
            int connectionId) {
        super(
                receiptId != null
                        ? Map.of("message", message, "receipt-id", receiptId)
                        : Map.of("message", message),
                details,
                connections,
                connectionId);
    }

    @Override
    public void process() {
        // Processing logic is not needed for this server-sent frame.
    }

    @Override
    public String getCommand() {
        return "ERROR";
    }
}
