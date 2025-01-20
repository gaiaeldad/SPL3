package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class DisconnectFrame extends Frame {
   DisconnectFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
      super(headers, body, connections, connectionId);
   }

   public void process() {
      boolean shouldDisconnect = true;

      try {
         this.checkReceipt();
      } catch (IOException var4) {
         shouldDisconnect = false;
         String[] SummaryAndBodyErr = var4.getMessage().split(":", 2);
         FrameUtil.handleError(this, SummaryAndBodyErr[0], SummaryAndBodyErr[1], this.connections, this.connectionId, (String)this.headers.get("receipt"));
      }

      if (shouldDisconnect) {
         FrameUtil.sendReceiptFrame((String)this.headers.get("receipt"), this.connections, this.connectionId);
         this.disconnect();
      }

   }

   private void disconnect() {
      this.connections.disconnect(this.connectionId);
   }

   private void checkReceipt() throws IOException {
      if (!this.headers.containsKey("receipt")) {
         throw new IOException("Frame doesn't contain receipt header:DISCONNECT frame must contain receipt header");
      }
   }

   public String getCommand() {
      return "DISCONNECT";
   }
}
    