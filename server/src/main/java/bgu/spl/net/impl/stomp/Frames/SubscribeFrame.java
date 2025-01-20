package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class SubscribeFrame extends Frame {
   SubscribeFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
      super(headers, body, connections, connectionId);
   }

   public void process() {
      boolean shouldSubscribe = true;

      try {
         this.checkDestination();
         this.checkId();
      } catch (IOException var4) {
         shouldSubscribe = false;
         String[] SummaryAndBodyErr = var4.getMessage().split(":", 2);
         FrameUtil.handleError(this, SummaryAndBodyErr[0], SummaryAndBodyErr[1], this.connections, this.connectionId, (String)this.headers.get("receipt"));
      }

      if (shouldSubscribe) {
         this.subscribe();
         if (this.headers.containsKey("receipt")) {
            FrameUtil.sendReceiptFrame((String)this.headers.get("receipt"), this.connections, this.connectionId);
         }
      }

   }

   private void subscribe() {
      this.connections.subscribe((String)this.headers.get("destination"), Integer.parseInt((String)this.headers.get("id")), this.connectionId);
   }

   private void checkId() throws IOException {
      if (!this.headers.containsKey("id")) {
         throw new IOException("Frame doesn't contain id header:SUBSCRIBE frame must contain id header");
      } else if (this.connections.getHandler(this.connectionId).getUser().getChannels().containsKey(Integer.parseInt((String)this.headers.get("id")))) {
         throw new IOException("id is not unique:You tried to subscribe to an already subscribed channel");
      }
   }

   private void checkDestination() throws IOException {
      if (!this.headers.containsKey("destination")) {
         throw new IOException("Frame doesn't contain destination header:SUBSCRIBE frame must contain destination header");
      }
   }

   public String getCommand() {
      return "SUBSCRIBE";
   }
}