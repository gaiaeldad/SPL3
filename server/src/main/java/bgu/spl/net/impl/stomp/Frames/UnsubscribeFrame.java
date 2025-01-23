package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class UnsubscribeFrame extends Frame {

    public UnsubscribeFrame(Map<String, String> headers, String body, Connections<String> connections,
            int connectionId) {
        super(headers, body, connections, connectionId);
    }

    @Override
    public void process() {
        boolean ShouldUnsubscribed = true;

        try {
            validateId(); // Ensure the "id" header is present and valid
        } catch (IOException e) {
            ShouldUnsubscribed = false;
            // Handle error by sending an ERROR frame to the client
            String[] errorDetails = e.getMessage().split(":", 2);
            FrameHelper.handleError(
                    this,
                    errorDetails[0],
                    errorDetails[1],
                    connections,
                    connectionId,
                    headers.get("receipt"));
        }

        if (ShouldUnsubscribed) {
            performUnsubscription(); // Remove the subscription

            // Send a RECEIPT frame if a "receipt" header was provided
            if (headers.containsKey("receipt")) {
                FrameHelper.sendReceiptFrame(headers.get("receipt"), connections, connectionId);
            }
        }
    }

    // Validate the "id" header to ensure it exists and is associated with the
    // client
    private void validateId() throws IOException {
        String id = headers.get("id");

        if (id == null) {
            throw new IOException("Missing Header:UNSUBSCRIBE frame must include the 'id' header.");
        }

        int subscriptionId = Integer.parseInt(id);

        // Check if the client is subscribed to the given subscription ID
        if (!connections.getHandler(connectionId).getUser().getChannels().containsKey(subscriptionId)) {
            throw new IOException(
                    "Invalid Subscription:You tried to unsubscribe from a subscription that does not exist.");
        }
    }

  // Remove the subscription from the server for the given client and ID
private void performUnsubscription() {
    int subscriptionId = Integer.parseInt(headers.get("id"));

    // Retrieve the channel associated with the subscriptionId
    Map<Integer, String> userSubscriptions = connections.getHandler(connectionId).getUser().getChannels();
    String channel = userSubscriptions.get(subscriptionId);

    if (channel != null) {
        // Remove the subscription from the user's subscriptions
        userSubscriptions.remove(subscriptionId);

        // Unsubscribe from the server
        connections.unsubscribe(channel, connectionId);
    
} 
    else {
    System.out.println("Subscription ID not found for connection: " + connectionId);

}
}


    @Override
    public String getCommand() {
        return "UNSUBSCRIBE";
    }
}
