package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.stomp.Frames.Frame;
import bgu.spl.net.impl.stomp.Frames.FrameParser;
import bgu.spl.net.srv.Connections;

public class StompMessageProtoclImpl<T> implements MessagingProtocol<T> {
   private boolean shouldTerminate = false;
   private Integer connectionId;
   private Connections<String> connections;

   public void start(int connectionId, Connections<T> connections) {
      this.connectionId = connectionId;
      this.connections = (Connections<String>) connections;
   }

   public void process(T msg) {
      System.out.println("Server received message: " + msg);
      Frame frame = FrameParser.parseFrame((String)msg, this.connections, this.connectionId);//checks what is the frame type 
      frame.process();//process by the frame type 
   }

   public boolean shouldTerminate() {
      return this.shouldTerminate;
   }

  
}
    