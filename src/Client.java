import core.RUDP;
import core.RUDPDataPacket;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        RUDP client = new RUDP();
        RUDPDataPacket data = new RUDPDataPacket(1, "Test from client");
        client.send(data, InetAddress.getLocalHost(), 5000);
        data = client.receive();
        System.out.println("Received data from server: " + data);
        client.close();
    }
}
