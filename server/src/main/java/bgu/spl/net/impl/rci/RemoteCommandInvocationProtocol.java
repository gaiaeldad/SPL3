package bgu.spl.net.impl.rci;

import bgu.spl.net.api.MessagingProtocol;
import java.io.Serializable;

public class RemoteCommandInvocationProtocol<T> implements MessagingProtocol<Serializable> {

    private T arg;

    public RemoteCommandInvocationProtocol(T arg) {
        this.arg = arg;
    }
//i changed this 
    @Override
    public void process(Serializable msg) {
        
        Serializable result = ((Command) msg).execute(arg);

        // Optionally, you can log or send the result via Connections if needed
        System.out.println("Command executed. Result: " + result);
        // Example: connections.send(connectionId, result);
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
    //i added this 
    @Override
    public void start(int connectionId, bgu.spl.net.srv.Connections<Serializable> connections) {
        // Initialize or reset any resources or state for the connection
        System.out.println("Connection started for connection ID: " + connectionId);
        // Optionally store connectionId or connections if needed
    }

}
