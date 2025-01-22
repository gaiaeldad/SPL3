package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import main.java.bgu.spl.net.impl.stomp.StompMessageProtoclImpl;




//need to change this this is just for now 
public class StompServer {
   public static void main(String[] args) {
      if (args.length != 2) {
         System.out.println("you must supply two arguments: <port>, <type_of_server> - tpc / reactor");
         System.exit(1);
      }


      int port = Integer.parseInt(args[0]);
      String serverType = args[1];

      if (serverType.equals("tpc")) {
         Server.threadPerClient(port, () -> {
            return new StompMessageProtoclImpl();
         }, StompEncoderDecoder::new).serve();
      } 
      else if (serverType.equals("reactor")) {
         Server.reactor(Runtime.getRuntime().availableProcessors(), port, () -> {
            return new StompMessageProtoclImpl();
         }, StompEncoderDecoder::new).serve();
      }
       else {
         System.out.println("you must supply on the second argument: <type_of_server> - tpc / reactor");
         System.exit(1);
      }

   }
}

