package bgu.spl.net.impl.stomp.Frames;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.Connections;

public class FrameParser {

    // Main method to parse a message into a Frame
    public static Frame parseFrame(String rawMessage, Connections<String> connections, int connectionId) {
        System.out.println("Parsing message into frame: " + rawMessage);
        Queue<String> messageLines = new ArrayDeque<>(Arrays.asList(rawMessage.split("\\n")));

        // Extract command type
        String command = messageLines.poll(); // Use poll() for safe removal
        if (command == null) {
            return null; // Invalid frame with no command
        }

        // Build headers from message lines
        ConcurrentHashMap<String, String> headerMap = extractHeaders(messageLines);

        // Extract body if available
        if (!messageLines.isEmpty() && messageLines.peek().isEmpty()) {
            messageLines.poll(); // Remove empty line separating headers and body
        }
        String messageBody = extractBody(messageLines);

        // Generate the appropriate Frame object based on the command
        return createFrame(command, connectionId, headerMap, messageBody, connections);
    }

    // Helper method to extract headers into a map
    private static ConcurrentHashMap<String, String> extractHeaders(Queue<String> lines) {
        ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();
        while (!lines.isEmpty() && !lines.peek().isEmpty()) {
            String[] keyValue = lines.poll().split(":", 2); // Split into key and value
            if (keyValue.length == 2) {
                headers.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return headers;
    }

    // Helper method to extract the body of the message
    private static String extractBody(Queue<String> lines) {
    if (lines == null || lines.isEmpty()) {
        return ""; // Return an empty string if the lines queue is null or empty
    }

    StringBuilder bodyBuilder = new StringBuilder();
    while (!lines.isEmpty() && !lines.peek().equals("\u0000")) {
        bodyBuilder.append(lines.poll()).append("\n");
    }

    // Remove the trailing newline character (if present) without relying on `strip()`
    int length = bodyBuilder.length();
    if (length > 0 && bodyBuilder.charAt(length - 1) == '\n') {
        bodyBuilder.setLength(length - 1);
    }

    return bodyBuilder.toString();
}


    // Factory method to create the appropriate Frame object
    private static Frame createFrame(String command, int connectionId, 
                                     ConcurrentHashMap<String, String> headers, 
                                     String body, 
                                     Connections<String> connections) {
        switch (command) {
            case "CONNECT":
                return new ConnectFrame(headers,body,connections,connectionId );
            case "CONNECTED":
                return new ConnectedFrame(body, headers, connections, connectionId);
            case "DISCONNECT":
                return new DisconnectFrame(headers,body,connections,connectionId );
            case "MESSAGE":
                return new MessageFrame(body, headers, connections, connectionId);
            case "RECEIPT":
                return new ReceiptFrame(body, headers, connections, connectionId);
            case "SEND":
                return new SendFrame(headers,body,connections,connectionId );
            case "SUBSCRIBE":
                return new SubscribeFrame(body, headers, connections, connectionId);
            case "UNSUBSCRIBE":
                return new UnsubscribeFrame(headers,body,connections,connectionId );
            case "ERROR":
                return new ErrorFrame(body, headers, connections, connectionId);
            default:
                // Return null for unrecognized command types
                return null;
        }
    }
}
