package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class UnsubscribeFrame extends Frame {
   UnsubscribeFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
      super(headers, body, connections, connectionId);
   }

   public void process() {
      boolean shouldUnsubscribe = true;

      try {
         this.checkId();
      } catch (IOException var4) {
         shouldUnsubscribe = false;
         String[] SummaryAndBodyErr = var4.getMessage().split(":", 2);
         FrameUtil.handleError(this, SummaryAndBodyErr[0], SummaryAndBodyErr[1], this.connections, this.connectionId, (String)this.headers.get("receipt"));
      }

      if (shouldUnsubscribe) {
         this.unsubscribe();
         if (this.headers.containsKey("receipt")) {
            FrameUtil.sendReceiptFrame((String)this.headers.get("receipt"), this.connections, this.connectionId);
         }
      }

   }

   private void unsubscribe() {
      this.connections.unsubscribe(Integer.parseInt((String)this.headers.get("id")), this.connectionId);
   }

   private void checkId() throws IOException {
      if (!this.headers.containsKey("id")) {
         throw new IOException("Frame doesn't contain id header:UNSUBSCRIBE frame must contain id header");
      } else if (!this.connections.getHandler(this.connectionId).getUser().getChannels().containsKey(Integer.parseInt((String)this.headers.get("id")))) {
         throw new IOException("you are not subscribed to this channelId:You tried to unsubscribe from a channel you are not subscribed to");
      }
   }

   public String getCommand() {
      return "UNSUBSCRIBE";
   }
}
    