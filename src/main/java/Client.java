import core.RUDP;
import core.RUDPDataPacket;
import core.RUDPDataPacketType;
import core.RUDPSocket;

import java.io.*;
import java.net.*;

import static java.lang.Thread.sleep;

public class Client {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        RUDP client = new RUDP();
        RUDPSocket socket = client.connect(InetAddress.getLocalHost(), 5000);
        for(int i = 0; i < 10; i++){
            RUDPDataPacket data = new RUDPDataPacket(i, "Test from client : " + i);
            socket.send(data);
            sleep(10000);
        }
//        data = client.receive();
//        System.out.println("Received data from server: " + data);
    }
}
