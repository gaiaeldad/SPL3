package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

public class StompProtocolImpl<T> implements StompMessagingProtocol<T> {

    private int connectionId;
    private Connections<T> connections;
    private boolean shouldTerminate = false;
    private boolean LoggedIn = false; // check if the user is logged in

    @Override
    public void start(int connectionId, Connections<T> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(T message) {
        // -------------לבדוק איך ההודעות נשלחות אם בסטרינג-------------------
        String frame = (String) message; // נניח שהמסגרת היא מחרוזת
        String[] lines = frame.split("\n");
        String command = lines[0];

        switch (command) {
            case "CONNECT":
                handleConnect(frame);
                break;
            case "SEND":
                handleSend(frame);
                break;
            case "SUBSCRIBE":
                handleSubscribe(frame);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(frame);
                break;
            case "DISCONNECT":
                handleDisconnect(frame);
                break;
            default:
                sendError("Unknown command: " + command);
                break;
        }
    }

    private void handleConnect(String frame) {
        // פרש את המסגרת ובדוק אם שם המשתמש והסיסמה נכונים
        String login = extractHeader(frame, "login");
        String passcode = extractHeader(frame, "passcode");

        if (isValidUser(login, passcode)) {
            connections.send(connectionId, "CONNECTED\nversion:1.2\n\n\u0000");
        } else {
            /// -----------לבדוק איך לשלוח את השגיאה והקבלה---------------
            sendError("Invalid login or passcode");
            // shouldTerminate = true;
        }
    }

    private void handleSend(String frame) {
        String destination = extractHeader(frame, "destination");
        if (!isSubscribedTo(destination)) {
            sendError("Not subscribed to destination: " + destination);
            System.out.println("Message not sent. Not subscribed to destination: " + destination);

            return;
        }
        String body = extractBody(frame);
        connections.send(destination, body);
        System.out.println("Message sent successfully to destination: " + destination);

    }

    private void handleSubscribe(String frame) {
        String destination = extractHeader(frame, "destination");
        String subscriptionId = extractHeader(frame, "id");
        connections.subscribe(destination, connectionId);
        System.out.println("Subscribed to destination: " + destination + " with subscription id: " + subscriptionId);
    }

    private void handleUnsubscribe(String frame) {
        String subscriptionId = extractHeader(frame, "id");
        connections.unsubscribe(subscriptionId, connectionId);
        System.out.println("Unsubscribed from subscription id: " + subscriptionId);
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
