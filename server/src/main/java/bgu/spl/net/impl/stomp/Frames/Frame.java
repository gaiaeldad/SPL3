package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

//abstract frame class 

public abstract class Frame {
   protected final ConcurrentHashMap<String, String> headers;// thread safe
   protected final String body;
   protected final Connections<String> connections;
   protected final int connectionId;

   Frame(Map<String, String> headers, String body, Connections<String> connections, int connectionId) {
      this.headers = new ConcurrentHashMap(headers);
      this.body = body;
      this.connections = connections;
      this.connectionId = connectionId;
   }

   @Override
   public String toString() {
      StringBuilder msg = new StringBuilder(getCommand()).append("\n");

      headers.forEach((key, value) -> msg.append(key).append(":").append(value).append("\n"));

      msg.append("\n").append(body);

      return msg.toString();
   }

   public abstract void process();

   public abstract String getCommand();
}