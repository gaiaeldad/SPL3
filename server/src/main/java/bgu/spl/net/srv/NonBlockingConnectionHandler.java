package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NonBlockingConnectionHandler<T> implements ConnectionHandler<T> {

    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; 
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    private final MessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
    private final SocketChannel chan;
    private final Reactor reactor;
    
    private User user = null;

    public NonBlockingConnectionHandler(
            MessageEncoderDecoder<T> reader,
            MessagingProtocol<T> protocol,
            SocketChannel chan,
            Reactor reactor)
             { 
        this.chan = chan;
        this.encdec = reader;
        this.protocol = protocol;
        this.reactor = reactor;
        
    }

    public Runnable continueRead() {
        ByteBuffer buf = leaseBuffer();

        boolean success = false;
        try {
            success = chan.read(buf) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (success) {
            buf.flip();
            return () -> {
                try {
                    while (buf.hasRemaining()) {
                        T nextMessage = encdec.decodeNextByte(buf.get());
                        if (nextMessage != null) {
                             protocol.process(nextMessage);
                           
                        }
                    }
                } finally {
                    releaseBuffer(buf);
                }
            };
        } else {
            releaseBuffer(buf);
            close();
            return null;
        }

    }

    public void close() {
        try {
            chan.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isClosed() {
        return !chan.isOpen();
    }

    public void continueWrite() {
    while (!writeQueue.isEmpty()) {
        try {
            if (!chan.isOpen()) { // Check if the channel is open
                System.err.println("Channel is closed. Stopping write.");
                close(); // Ensure the channel is closed and clean up
                return;
            }

            ByteBuffer top = writeQueue.peek();
            int bytesWritten = chan.write(top);
            System.out.println("Bytes written: " + bytesWritten);

            if (top.hasRemaining()) {
                // Buffer still has data to write, exit the loop
                System.out.println("Buffer not fully written, remaining bytes: " + top.remaining());
                return;
            } else {
                // Buffer fully written, remove it from the queue
                writeQueue.remove();
                System.out.println("Buffer fully written and removed from queue.");
            }
        } catch (ClosedChannelException e) {
            System.err.println("Attempted to write to a closed channel: " + e.getMessage());
            close();
            return;
        } catch (IOException ex) {
            System.err.println("I/O error during write: " + ex.getMessage());
            close();
            return;
        }
    }

    // If write queue is empty, switch to read mode
    if (writeQueue.isEmpty()) {
        if (protocol.shouldTerminate()) {
            System.out.println("Protocol indicated termination. Closing channel.");
            close();
        } else {
            System.out.println("Write queue empty. Switching to OP_READ.");
            reactor.updateInterestedOps(chan, SelectionKey.OP_READ);
        }
    }
}


    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) {
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
        }

        buff.clear();
        return buff;
    }

    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }


    //unlike the blocking can write firectly to the output stream 
    //need to use a buffer to manage the I\O
    @Override
    public void send(T msg) {
    if (msg != null) {
    writeQueue.add(ByteBuffer.wrap(encdec.encode(msg)));
    reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);}
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
