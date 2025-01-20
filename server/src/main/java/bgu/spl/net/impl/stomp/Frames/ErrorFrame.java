package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class ErrorFrame extends Frame {
   ErrorFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
      super(headers, body, connections, connectionId);
   }

   public void process() {
   }

   public String getCommand() {
      return "ERROR";
   }
}
    