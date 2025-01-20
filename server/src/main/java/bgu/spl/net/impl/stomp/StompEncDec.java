package main.java.bgu.spl.net.impl.stomp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import bgu.spl.net.api.MessageEncoderDecoder;


//this looks good 
public class StompEncDec implements MessageEncoderDecoder<String> {

    private byte[] buffer = new byte[1 << 10]; // Start with 1KB buffer
    private int length = 0; // Current length of the buffer

    @Override
    public String decodeNextByte(byte nextByte) {
        if (nextByte == 0) { // Assuming '\n' marks the end of a message
            return popString(); // Return the complete message
        }
        pushByte(nextByte); // Add the byte to the buffer
        return null; // Message not complete yet
    }

    @Override
    public byte[] encode(String message) {
        // Append '\u0000' as the delimiter and convert the message to bytes
        return (message + '\u0000').getBytes(StandardCharsets.UTF_8);
    }

    private String popString() {
        // Convert the buffer to a String using UTF-8, reset the buffer, and return the message
        String result = new String(buffer, 0, length, StandardCharsets.UTF_8);
        length = 0; // Reset buffer
        return result;
    }

    private void pushByte(byte nextByte) {
        // Resize the buffer if it's full
        if (length >= buffer.length) {
            buffer = Arrays.copyOf(buffer, length * 2); // Double the size of the buffer
        }
        buffer[length++] = nextByte; // Add the new byte to the buffer
    }
}