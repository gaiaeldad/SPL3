package bgu.spl.net.impl.stomp.Frames;

import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.srv.Connections;

public class FrameParser {
   //im not sure what we need to do here and how- we need to check!!!!!!!!!
   /////////////////////////////////
   /// 
    public static Frame parse(String message, Connections<String> connections, int connectionId) {
        String[] lines = message.split("\n");
        String command = lines[0].trim(); // The first line is the command
        Map<String, String> headers = extractHeaders(lines);
        String body = extractBody(lines);

        return createFrame(command, headers, body, connections, connectionId);
    }

    
    private static Map<String, String> extractHeaders(String[] lines) {
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) { // Stop when we encounter an empty line (end of headers)
                break;
            }
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                headers.put(parts[0].trim(), parts[1].trim());
            }
        }
        return headers;
    }

    private static String extractBody(String[] lines) {
        StringBuilder bodyBuilder = new StringBuilder();
        boolean isBody = false;
        for (String line : lines) {
            if (line.isEmpty()) {
                isBody = true; // Empty line marks the start of the body
                continue;
            }
            if (isBody) {
                bodyBuilder.append(line).append("\n");
            }
        }
        return bodyBuilder.toString().trim();
    }

    private static Frame createFrame(String command, Map<String, String> headers, String body, Connections<String> connections, int connectionId) {
        switch (command) {
            case "SEND":
                return new SendFrame(headers, body, connections, connectionId);
            case "SUBSCRIBE":
                return new SubscribeFrame(body, headers, connections, connectionId);
            case "UNSUBSCRIBE":
                return new UnsubscribeFrame(headers, body, connections, connectionId);
            case "CONNECT":
                return new ConnectFrame(headers, body, connections, connectionId);
            case "DISCONNECT":
                return new DisconnectFrame(headers, body, connections, connectionId);
            default:
                throw new IllegalArgumentException("Unknown command: " + command);
        }
    }
}
