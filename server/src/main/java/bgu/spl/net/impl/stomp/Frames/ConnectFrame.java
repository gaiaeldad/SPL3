package main.java.bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.util.Map;

public class ConnectFrame extends Frame {
   ConnectFrame(String body, Map<String, String> headers, Connections<String> connections, int connectionId) {
      super(headers, body, connections, connectionId);
   }

   public void process() {
      boolean shouldLogin = true;

      try {
         this.checkAcceptVersion();
         this.checkHost();
         this.checkLogin();
      } catch (IOException var4) {
         shouldLogin = false;
         String[] SummaryAndBodyErr = var4.getMessage().split(":", 2);
         FrameUtil.handleError(this, SummaryAndBodyErr[0], SummaryAndBodyErr[1], this.connections, this.connectionId, (String)this.headers.get("receipt"));
      }

      if (shouldLogin) {
         this.login();
         FrameUtil.sendConnectedFrame((String)this.headers.get("accept-version"), this.connectionId, this.connections);
         if (this.headers.containsKey("receipt")) {
            FrameUtil.sendReceiptFrame((String)this.headers.get("receipt"), this.connections, this.connectionId);
         }
      }

   }

   private void login() {
      this.connections.login(this.connectionId, (String)this.headers.get("login"), (String)this.headers.get("passcode"));
   }

   private void checkLogin() throws IOException {
      if (this.headers.containsKey("login") && this.headers.containsKey("passcode")) {
         if (!this.isLegalLoginInfo((String)this.headers.get("login"), (String)this.headers.get("passcode"))) {
            throw new IOException("Password does not match UserName:User " + (String)this.headers.get("login") + "'s password is diffrent than what you inserted");
         } else if (this.isUserLogedIn((String)this.headers.get("login"), (String)this.headers.get("passcode"))) {
            throw new IOException("User already logged in:User " + (String)this.headers.get("login") + "is logged in somewhere else");
         }
      } else {
         throw new IOException("Frame doesn't contain login or password header:CONNECT frame must contain login and password headers");
      }
   }

   private boolean isUserLogedIn(String userName, String password) {
      return this.connections.isUserLogedIn(userName, password);
   }

   private boolean isLegalLoginInfo(String userName, String password) {
      return this.connections.isLegalLoginInfo(userName, password);
   }

   private void checkHost() throws IOException {
      if (!this.headers.containsKey("host")) {
         throw new IOException("Frame doesn't contain host header:CONNECT frame must contain host header, please use" + FrameUtil.HOST);
      } else if (!((String)this.headers.get("host")).equals(FrameUtil.HOST)) {
         throw new IOException("Frame doesn't match host header:In CONNECT frame the host must be equal to: 1.2");
      }
   }

   private void checkAcceptVersion() throws IOException {
      if (!this.headers.containsKey("accept-version")) {
         throw new IOException("Frame doesn't contain accept-version header:CONNECT frame must contain accept-version, we currently support version: 1.2");
      } else if (!((String)this.headers.get("accept-version")).equals("1.2")) {
         throw new IOException("Frame doesn't match accept-version header:In CONNECT frame the accept-version must be equal to: 1.2");
      }
   }

   public String getCommand() {
      return "CONNECT";
   }
}