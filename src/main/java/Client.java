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
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String s;
        RUDPSocket socket = client.connect(InetAddress.getLocalHost(), 5000);
        System.out.println("Enter packet number: ");
        int packetNumber = 0;
        while (!(s = br.readLine()).equals("-1")) {
            packetNumber = Integer.parseInt(s);
            RUDPDataPacket data = new RUDPDataPacket(packetNumber, "Test from client : " + packetNumber);
            socket.send(data);
            System.out.println("Enter packet number: ");
        }
        socket.send(new RUDPDataPacket(++packetNumber, RUDPDataPacketType.EOD));
        while (!(s = br.readLine()).equals("-1")) {
            packetNumber = Integer.parseInt(s);
            RUDPDataPacket data = new RUDPDataPacket(packetNumber, "Test from client : " + packetNumber);
            socket.send(data);
            System.out.println("Enter packet number: ");
        }
        socket.send(new RUDPDataPacket(++packetNumber, RUDPDataPacketType.EOD));
//        data = client.receive();
//        System.out.println("Received data from server: " + data);
    }
}
