package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class ReceiptFrame extends Frame {
    public ReceiptFrame(String receiptId, Connections<String> connections, int connectionId) {
        super(Map.of("receipt-id", receiptId), "", connections, connectionId);
    }

    @Override
    public void process() {
        // Processing logic is not needed for this server-sent frame.
    }

    @Override
    public String getCommand() {
        return "RECEIPT";
    }
}
