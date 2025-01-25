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

    // Validate the "id" header to ensure it exists and is unique for the client
    private void validateSubscriptionId() throws IOException {
        String id = headers.get("id");

        if (id == null) {
            throw new IOException("Missing Header:SUBSCRIBE frame must include the 'id' header.");
        }

        int subscriptionId = Integer.parseInt(id);

        if (connections.getHandler(connectionId).getUser().getSubscriptions().containsKey(subscriptionId)) {
            throw new IOException(
                    "Duplicate Subscription:You are already subscribed to this channel with id '" + id + "'.");
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
                Integer.parseInt(headers.get("id")),
                connectionId,
                headers.get("destination"));
    }

    @Override
    public String getCommand() {
        return "SUBSCRIBE";
    }
}
