package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class DisconnectFrame extends Frame {

    public DisconnectFrame(Map<String, String> headers, String body, Connections<String> connections,
            int connectionId) {
        super(headers, body, connections, connectionId);
    }

    @Override
    public void process() {
        boolean Disconnect = true;

        try {
            validateReceipt(); // Ensure the "receipt" header is present
        } catch (IOException e) {
            Disconnect = false;
            // Handle error by sending an ERROR frame with the appropriate details
            String[] errorDetails = e.getMessage().split(":", 2);
            FrameHelper.handleError(
                    this,
                    errorDetails[0],
                    errorDetails[1],
                    connections,
                    connectionId,
                    headers.get("receipt") // Optional "receipt" header
            );
        }

        if (Disconnect) {
            // Send a RECEIPT frame if the "receipt" header exists
            if (headers.containsKey("receipt")) {
                FrameHelper.sendReceiptFrame(headers.get("receipt"), connections, connectionId);
            }

            performDisconnection(); // Disconnect the client from the server
        }
    }

    // Validate the "receipt" header to ensure it is included in the frame
    private void validateReceipt() throws IOException {
        if (!headers.containsKey("receipt")) {
            throw new IOException("Missing Header:DISCONNECT frame must include the 'receipt' header.");
        }
    }

    // Disconnect the client by invoking the appropriate method in Connections
    private void performDisconnection() {
        connections.disconnect(connectionId);
    }

    @Override
    public String getCommand() {
        return "DISCONNECT";
    }
}
