package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class FrameParser {
   public static Frame Parse(String msg, Connections<String> connections, int connectionId) {
      Queue<String> msgLinesQueue = new LinkedList(Arrays.asList(msg.split("\\n")));
      String frameCommandType = (String)msgLinesQueue.remove();
      Map<String, String> headers = getHeaders(msgLinesQueue);
      if (!msgLinesQueue.isEmpty() && ((String)msgLinesQueue.peek()).equals("")) {
         msgLinesQueue.remove();
      }

      String body = getBody(msgLinesQueue);
      Frame frame = BuildFrameByCommandType(frameCommandType, headers, body, connections, connectionId);
      return frame;
   }

   private static Map<String, String> getHeaders(Queue<String> msgLinesQueue) {
      ConcurrentHashMap headers = new ConcurrentHashMap();

      while(!msgLinesQueue.isEmpty() && !((String)msgLinesQueue.peek()).equals("")) {
         String[] keyVal = ((String)msgLinesQueue.remove()).split(":");
         headers.put(keyVal[0], keyVal[1]);
      }

      return headers;
   }

   private static String getBody(Queue<String> msgLinesQueue) {
      String body;
      for(body = ""; !msgLinesQueue.isEmpty() & msgLinesQueue.peek() != "\u0000"; body = body + (String)msgLinesQueue.remove() + "\n") {
      }

      return body;
   }

   private static Frame BuildFrameByCommandType(String frameCommandType, Map<String, String> headers, String body, Connections<String> connections, int connectionId) {
      byte var6 = -1;
      switch(frameCommandType.hashCode()) {
      case -2087582999:
         if (frameCommandType.equals("CONNECTED")) {
            var6 = 1;
         }
         break;
      case -1558724943:
         if (frameCommandType.equals("UNSUBSCRIBE")) {
            var6 = 7;
         }
         break;
      case -993530582:
         if (frameCommandType.equals("SUBSCRIBE")) {
            var6 = 6;
         }
         break;
      case 2541448:
         if (frameCommandType.equals("SEND")) {
            var6 = 5;
         }
         break;
      case 66247144:
         if (frameCommandType.equals("ERROR")) {
            var6 = 8;
         }
         break;
      case 1015497884:
         if (frameCommandType.equals("DISCONNECT")) {
            var6 = 2;
         }
         break;
      case 1669334218:
         if (frameCommandType.equals("CONNECT")) {
            var6 = 0;
         }
         break;
      case 1672907751:
         if (frameCommandType.equals("MESSAGE")) {
            var6 = 3;
         }
         break;
      case 1800273432:
         if (frameCommandType.equals("RECEIPT")) {
            var6 = 4;
         }
      }

      switch(var6) {
      case 0:
         return new ConnectFrame(body, headers, connections, connectionId);
      case 1:
         return new ConnectedFrame(body, headers, connections, connectionId);
      case 2:
         return new DisconnectFrame(body, headers, connections, connectionId);
      case 3:
         return new MessageFrame(body, headers, connections, connectionId);
      case 4:
         return new ReceiptFrame(body, headers, connections, connectionId);
      case 5:
         return new SendFrame(body, headers, connections, connectionId);
      case 6:
         return new SubscribeFrame(body, headers, connections, connectionId);
      case 7:
         return new UnsubscribeFrame(body, headers, connections, connectionId);
      case 8:
         return new ErrorFrame(body, headers, connections, connectionId);
      default:
         return null;
      }
   }
}