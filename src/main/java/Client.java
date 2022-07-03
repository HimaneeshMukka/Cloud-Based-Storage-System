import core.RUDP;
import core.RUDPDataPacket;
import core.RUDPDataPacketType;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        RUDP client = new RUDP();
        client.send(new RUDPDataPacket(0, RUDPDataPacketType.HANDSHAKE), InetAddress.getLocalHost(), 5000);
//        for(int i = 0; i < 10; i++){
//            RUDPDataPacket data = new RUDPDataPacket(i, "Test from client");
//            client.send(data, InetAddress.getLocalHost(), 5000);
//        }
//        data = client.receive();
//        System.out.println("Received data from server: " + data);
    }
}
