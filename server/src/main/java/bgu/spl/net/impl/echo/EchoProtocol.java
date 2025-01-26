package bgu.spl.net.impl.echo;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.time.LocalDateTime;

public class EchoProtocol implements MessagingProtocol<String> {

    private boolean shouldTerminate = false;

    //i chnaged this 
    @Override
    public void process(String msg) {
        shouldTerminate = "bye".equals(msg);
        System.out.println("[" + LocalDateTime.now() + "]: " + msg);

        // Process the message and log the echo, but do not return it.
        String response = createEcho(msg);
        System.out.println("Response: " + response);
        // You could send this response via your connection mechanism if needed.
    }

    private String createEcho(String message) {
        String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
        return message + " .. " + echoPart + " .. " + echoPart + " ..";
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    //i added this 
       @Override
    public void start(int connectionId, Connections<String> connections) {
        // Initialize or reset any resources or state for the connection
        System.out.println("Connection started for connection ID: " + connectionId);
        // Optionally store connectionId or connections if needed
    }

}
