import core.RUDP;
import core.RUDPDataPacket;

import java.io.*;

import static java.lang.Thread.sleep;

public class Server {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        RUDP server = new RUDP(5000);
        server.listen();
        System.out.println("Server listening on port 5000");

        // Sender
//        new Thread(() -> {
//            try {
//                int sequenceNumber = 1;
//                while (true) {
//                    System.out.println("Sending data..." + sequenceNumber);
//                    boolean flag = server.send(new RUDPDataPacket(sequenceNumber, "Test from server"));
//                    System.out.println("Sent? " + flag);
//                    if(flag){
//                        System.out.println("Sent data to client: " + sequenceNumber);
//                        sequenceNumber++;
//                    }
//                    else System.out.println("Failed to send data: " + sequenceNumber);
//                    sleep(10000);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
    }
}
