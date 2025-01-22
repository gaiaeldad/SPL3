package main.java.bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.stomp.Frames.Frame;
import bgu.spl.net.impl.stomp.Frames.FrameParser;
import bgu.spl.net.srv.Connections;
//////this is the new version 20.1 16:37 
/// also not sure under what folder it should be 
/// 
public class StompMessageProtoclImpl<T> implements MessagingProtocol<T> {
   private boolean shouldTerminate = false;
   private Integer connectionId;
   private Connections<String> connections;

   public void start(int connectionId, Connections<T> connections) {
      this.connectionId = connectionId;
      this.connections = (Connections<String>) connections;
   }

   public void process(T msg) {// we need to add the parser 
      Frame frame = FrameParser.Parse(msg, this.connections, this.connectionId);//check what is the frame type 
      frame.process();//process by the frame type 
   }

   public boolean shouldTerminate() {
      return this.shouldTerminate;
   }

  
}
    