package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class ConnectFrame extends Frame {

    public ConnectFrame(Map<String, String> headers, String body, Connections<String> connections, int connectionId) {
        super(headers, body, connections, connectionId);
    }

    @Override
    public void process() {
        boolean loginSuccessful = true;

        try {
            validateAcceptVersion(); // Validate the "accept-version" header
            validateHost(); // Validate the "host" header
            validateCredentials(); // Validate "login" and "passcode" headers
        } catch (IOException e) {
            // If validation fails, handle the error and stop further processing
            loginSuccessful = false;
            String[] errorDetails = e.getMessage().split(":", 2);// allows separate the error type from the error
                                                                 // description
            FrameHelper.ProcessError(
                    this,
                    errorDetails[0],
                    errorDetails[1],
                    connections,
                    connectionId,
                    headers.get("receipt") // Use the "receipt" header if present
            );
        }

        if (loginSuccessful) {
            performLogin(); // Log the user in
            FrameHelper.sendConnectedFrame(headers.get("accept-version"), connectionId, connections); // Send
                                                                                                      // "CONNECTED"
                                                                                                      // frame

            // If a "receipt" header is provided, send a "RECEIPT" frame
            if (headers.containsKey("receipt")) {
                FrameHelper.sendReceiptFrame(headers.get("receipt"), connections, connectionId);
            }
        }
    }

    // Validate the "accept-version" header to ensure it is set to "1.2"
    private void validateAcceptVersion() throws IOException {
        String acceptVersion = headers.get("accept-version");
        if (acceptVersion == null) {
            throw new IOException("Missing Header:CONNECT frame must include the 'accept-version' header.");
        }
        if (!acceptVersion.equals("1.2")) {
            throw new IOException("Invalid Version:Only STOMP version 1.2 is supported.");
        }
    }

    // Validate the "host" header to ensure it matches the expected host
    private void validateHost() throws IOException {
        String host = headers.get("host");
        if (host == null) {
            throw new IOException("Missing Header:CONNECT frame must include the 'host' header.");
        }
        //made a chnage here 25.1 
        if (!host.equals(FrameHelper.HOST)&& !host.equals("127.0.0.1")) {
            throw new IOException("Invalid Host:The 'host' header must match: " + FrameHelper.HOST);
        }
    }

    // Validate the "login" and "passcode" headers and check if the user is allowed to log in 
    private void validateCredentials() throws IOException {
        String login = headers.get("login");
        String passcode = headers.get("passcode");

        if (login == null || passcode == null) {
            throw new IOException(
                    "Missing Credentials:CONNECT frame must include both 'login' and 'passcode' headers.");
        }

        if (!isValidLogin(login, passcode)) {
            throw new IOException("Authentication Failed:The provided credentials are invalid.");
        }

        if (isUserLoggedIn(login)) {
            throw new IOException("User Already Logged In:User '" + login + "' is already logged in.");
        }
    }

    // Perform the login by notifying the connections object
    private void performLogin() {
        connections.login(connectionId, headers.get("login"), headers.get("passcode"));
    }

    // Check if the login and passcode are valid
    private boolean isValidLogin(String login, String passcode) {
        return connections.isLegalCredentials(login, passcode);
    }

    // Check if the user is already logged in
    private boolean isUserLoggedIn(String login) {
        return connections.isUserLogedIn(login);
    }

    @Override
    public String getCommand() {
        return "CONNECT";
    }
}
