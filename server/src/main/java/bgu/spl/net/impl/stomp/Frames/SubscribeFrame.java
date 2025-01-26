package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class SubscribeFrame extends Frame {

    public SubscribeFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
        super(headers, body, connections, connectionId);
    }

    @Override
    public void process() {
        boolean subscriptionSuccessful = true;

        try {
            validateDestination(); // Validate the "destination" header
            validateSubscriptionId(); // Validate the "id" header
        } catch (IOException e) {
            subscriptionSuccessful = false;
            // Handle error by sending an ERROR frame with appropriate details
            String[] errorDetails = e.getMessage().split(":", 2);
            FrameHelper.ProcessError(
                    this,
                    errorDetails[0],
                    errorDetails[1],
                    connections,
                    connectionId,
                    headers.get("receipt") // Optional "receipt" header
            );
        }

        if (subscriptionSuccessful) {
            performSubscription(); // Add the subscription to the server
            // Send a RECEIPT frame if the client included a "receipt" header
            if (headers.containsKey("receipt")) {
                FrameHelper.sendReceiptFrame(headers.get("receipt"), connections, connectionId);
            }
        }
    }

    private void validateSubscriptionId() throws IOException {
        if (!this.headers.containsKey("id")) {
           throw new IOException("Frame doesn't contain id header:SUBSCRIBE frame must contain id header");
        } else if (this.connections.getHandler(this.connectionId).getUser().getSubscriptions().containsKey(Integer.parseInt((String)this.headers.get("id")))) {
           throw new IOException("id is not unique:You tried to subscribe to an already subscribed channel");
        }
     }

    // Validate the "destination" header to ensure it exists
    private void validateDestination() throws IOException {
        if (!headers.containsKey("destination")) {
            throw new IOException("Missing Header:SUBSCRIBE frame must include the 'destination' header.");
        }
    }

    // Add the subscription for the client to the specified destination
    private void performSubscription() {
        connections.subscribe(
                Integer.parseInt(headers.get("id")),/////what is this 
                connectionId,
                headers.get("destination"));
    }

    @Override
    public String getCommand() {
        return "SUBSCRIBE";
    }
}
