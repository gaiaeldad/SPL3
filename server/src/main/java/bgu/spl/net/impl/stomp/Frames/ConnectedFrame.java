package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Map;

public class ConnectedFrame extends Frame {
   public ConnectedFrame(String version, Connections<String> connections, int connectionId) {
       super(Map.of("version", version), "", connections, connectionId);
   }

   @Override
   public void process() {
       // Processing logic is usually not required for server-sent frames.
   }

   @Override
   public String getCommand() {
       return "CONNECTED";
   }
}

  