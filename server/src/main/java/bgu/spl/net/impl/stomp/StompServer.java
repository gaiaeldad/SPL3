package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;


public class StompServer {

    public static void main(String[] args) {
        if (args.length < 2) {//check for port and mode 
            System.out.println("Usage: StompServer <port> <mode>");
            System.out.println("Modes: reactor, thread-per-client");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number: " + args[0]);
            return;
        }

        String mode = args[1];

        switch (mode) {
            case "reactor":
                Server.reactor(
                    Runtime.getRuntime().availableProcessors(),
                    port,
                    () -> new StompMessageProtoclImpl(), // Lambda for protocol factory
                    () -> new StompEncDec()              // Lambda for encoder/decoder factory
                ).serve();
                break;

            case "tpc":
                Server.threadPerClient(
                    port,
                    () -> new StompMessageProtoclImpl(), // Lambda for protocol factory
                    () -> new StompEncDec()              // Lambda for encoder/decoder factory
                ).serve();
                break;

            default:
                System.out.println("Unknown mode: " + mode);
                System.out.println("Modes: reactor, thread-per-client");
                break;
        }
    }
}

