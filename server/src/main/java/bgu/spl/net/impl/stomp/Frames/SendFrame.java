    package bgu.spl.net.impl.stomp.Frames;

    import bgu.spl.net.srv.Connections;
    import java.io.IOException;
    import java.util.LinkedList;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;

    public class SendFrame extends Frame {

        public SendFrame(Map<String, String> headers, String body, Connections<String> connections, int connectionId) {
            super(headers, body, connections, connectionId);
        }

        @Override
        public void process() {
            boolean sendMessage = true;

            try {
                validateDestination(); // Ensure the "destination" header is valid
            } catch (IOException e) {
                sendMessage = false;
                // Handle error by sending an ERROR frame
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

            if (sendMessage) {
                forwardMessage(); // Forward the message to all subscribed clients

                // If the client provided a "receipt" header, send a RECEIPT frame
                if (headers.containsKey("receipt")) {
                    FrameHelper.sendReceiptFrame(headers.get("receipt"), connections, connectionId);
                }
            }
        }

        // Validate the "destination" header to ensure the channel exists and the user
        // is subscribed
        private void validateDestination() throws IOException {
            if (!headers.containsKey("destination")) {
                throw new IOException("Frame doesn't contain destination header:SEND frame must contain destination header");
            }
        
            String destination = headers.get("destination"); // Keep the full destination
        
            System.out.println("[Debug] Validating destination: " + destination);
        
            if (!connections.isDestinationLegal(destination, connectionId)) {
                throw new IOException("Illegal destination: Channel doesn't exist or you're not subscribed to it");
            }
        
            System.out.println("[Debug] Destination validated successfully for channel: " + destination);
        }
        
        
        

        // Forward the message to all clients subscribed to the destination
        private void forwardMessage() {
            // Retrieve the list of connection IDs subscribed to the channel
            //changed this 
            LinkedList<Integer> subscribers = connections.getConnectionIdsOfChannel(headers.get("destination"));
            System.out.println("[Debug] Forwarding message to channel: " + headers.get("destination"));
            System.out.println("[Debug] Subscribers: " + subscribers);
        
            // For each subscriber, send the message
            for (int subscriberId : subscribers) {
                System.out.println("[Debug] Sending message to connectionId: " + subscriberId);
                // Create headers for the MessageFrame
                Map<String, String> messageHeaders = new ConcurrentHashMap<>();
                messageHeaders.put("destination", headers.get("destination"));
                messageHeaders.put("message-id", String.valueOf(connections.getAndIncMsgIdCounter()));
                messageHeaders.put("subscription", String.valueOf(subscriberId));
        
                // Create and send the MessageFrame
                String messageFrame = new MessageFrame(
                        body, // Message body
                        messageHeaders,
                        connections,
                        subscriberId
                ).toString();
        
                connections.send(subscriberId, messageFrame);
            }
        }

        // Create headers for the forwarded message, including a unique "message-id"
        private Map<String, String> createMessageHeaders(int messageId) {
            Map<String, String> MsgHeaders = new ConcurrentHashMap<>(headers);
            MsgHeaders.put("message-id", String.valueOf(messageId));
            addSubscriptionHeader(MsgHeaders);
            return MsgHeaders;
        }

        // Add the "subscription" header based on the client's subscriptions
        private void addSubscriptionHeader(Map<String, String> msgHeaders) {
            Map<Integer, String> userSubscriptions = connections.getHandler(connectionId).getUser().getSubscriptions();


            // Find the subscription ID for the destination and add it to the headers
            for (Map.Entry<Integer, String> entry : userSubscriptions.entrySet()) {
                if (headers.get("destination").equals(entry.getValue())) {
                    msgHeaders.put("subscription", String.valueOf(entry.getKey()));
                    break;
                }
            }
        }

        @Override
        public String getCommand() {
            return "SEND";
        }
    }
