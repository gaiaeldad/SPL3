package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final MessagingProtocol<T> protocol;// make sure this works as expected 
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    //we added - make sure this is the correct user 
    private User user; // User associated with this connection

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol,User user) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.user = user;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { // just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                    
                }
                // i added this 22.1 
            //     if (isLoginMessage(nextMessage)) {
            //         String username = getUsername(nextMessage);
            //         String password = extractPassword(nextMessage);

            //         // Validate login (you'll need to implement this)
            //         if (validateCredentials(username, password)) {
            //             user.setUsername(username);
            //             user.setPassword(password);
            //             user.setLoggedIn(true);
            //             System.out.println("User logged in: " + username);
            //         } else {
            //             System.out.println("Invalid login attempt");
            //         }
            //     }
            // }


            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }


    // blocking connection handler write directly to an output stream
    @Override
    public void send(T msg) {
        if (msg != null) {
            try{
                out.write(encdec.encode(msg));
                out.flush();
            } catch (IOException e){
                System.out.println(e);
            }
            
        }
    }
    @Override
    public User getUser() {
        return user;
    }
    @Override
    public void setUser(User user) {
    this.user = user;
}

    @Override
    public MessagingProtocol<T> getProtocol() {
    return protocol;
}
    
    

}
