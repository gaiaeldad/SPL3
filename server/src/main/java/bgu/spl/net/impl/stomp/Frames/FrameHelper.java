package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrameHelper {
   public static final String STOMP_VERSION = "1.2";
   public static final Object HOST = "stomp.cs.bgu.ac.il";

   public static void sendReceiptFrame(String receiptId, Connections<String> connections, int connectionId) {
      connections.send(connectionId,
            (new ReceiptFrame("", createReceiptHeaders(receiptId), connections, connectionId)).toString());
   }

   private static Map<String, String> createReceiptHeaders(String receiptId) {
      Map<String, String> receiptHeaders = new ConcurrentHashMap();
      receiptHeaders.put("receipt-id", receiptId);
      return receiptHeaders;
   }

   public static void handleError(Frame frameCausedErr, String errSummary, String errExplain,
         Connections<String> connections, int connectionId, String receiptId) {
      sendErrorFrame(frameCausedErr, errSummary, errExplain, connections, connectionId, receiptId);
      ConnectionHandler<String> handler = connections.getHandler(connectionId);
      connections.disconnect(connectionId);

      try {
         handler.close();
      } catch (IOException var8) {
         var8.printStackTrace();
      }

   }

   public static void sendErrorFrame(Frame frameCausedErr, String errHeader, String errExplain,
         Connections<String> connections, int connectionId, String receiptId) {
      connections.send(connectionId, (new ErrorFrame(createErrBody(frameCausedErr, errExplain),
            createErrHeaders(receiptId, errHeader), connections, connectionId)).toString());
   }

   private static String createErrBody(Frame frameCausedErr, String errExplain) {
      String errBody = "";
      errBody = errBody + "The message: \\n";
      errBody = errBody + "----- \\n";
      errBody = errBody + frameCausedErr.toString();
      errBody = errBody + "----- \\n";
      errBody = errBody + errExplain;
      return errBody;
   }

   private static Map<String, String> createErrHeaders(String receiptId, String errHeader) {
      Map<String, String> errHeaders = new ConcurrentHashMap();
      if (receiptId != null) {
         errHeaders.put("receipt-id", receiptId);
      }

      errHeaders.put("message", errHeader);
      return errHeaders;
   }

   public static void sendConnectedFrame(String StompVersion, int connectionId, Connections<String> connections) {
      connections.send(connectionId,
            (new ConnectedFrame("", createConnectedHeaders(StompVersion), connections, connectionId)).toString());
   }

   private static Map<String, String> createConnectedHeaders(String stompVersion) {
      Map<String, String> headers = new ConcurrentHashMap();
      headers.put("version", stompVersion);
      return headers;
   }

   public static void sendMessageFrame(String msg, String subscriptionId, String channelName, String messageId,
         int connectionId, Connections<String> connections) {
      connections.send(connectionId, (new MessageFrame(msg,
            createMessageHeaders(subscriptionId, messageId, channelName), connections, connectionId)).toString());
   }

   private static Map<String, String> createMessageHeaders(String subscriptionId, String messageId,
         String channelName) {
      Map<String, String> headers = new ConcurrentHashMap();
      headers.put("subscription", subscriptionId);
      headers.put("message-id", messageId);
      headers.put("destination", channelName);
      return headers;
   }
}
