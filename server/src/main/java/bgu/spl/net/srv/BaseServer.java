package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<MessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    
    private ConnectionsImpl<T> connections;
    private int connectionIdCount;

    public BaseServer(
            int port,
            Supplier<MessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections = new ConnectionsImpl<T>();
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; 

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
        
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                    clientSock,
                    (MessageEncoderDecoder<T>) this.encdecFactory.get(),
                    (MessagingProtocol<T>) this.protocolFactory.get()
                );
                
                connections.addConnection(connectionIdCount,handler);
    
                handler.getProtocol().start(this.connectionIdCount,this.connections);
                this.connections.addConnectionHandler(handler, this.connectionIdCount);

                connectionIdCount++;
                
                execute(handler);
            }
        } catch (IOException ex) {
            System.out.println("got ecxpetion in base server");
        }

        System.out.println("server closed!!!");
    }


    
    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
