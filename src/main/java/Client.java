import core.*;

import java.io.*;
import java.net.*;
import java.util.List;

import static java.lang.Thread.sleep;

public class Client {
    static FileSystem fs = new FileSystem("/Users/skreweverything/client1/");
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        RUDP client = new RUDP();
        RUDPSocket socket = client.connect(InetAddress.getLocalHost(), 5000);
        firstSync(socket);
//        data = client.receive();
//        System.out.println("Received data from server: " + data);
    }

    public static void firstSync(RUDPSocket socket) throws IOException, InterruptedException {
        System.out.println("Sending first sync...");
        fs.reloadCachedFiles();
        socket.send(new RUDPDataPacket(0, fs.getCachedFiles(), ObjectType.LIST_FILEMETA));
        socket.send(new RUDPDataPacket(1, RUDPDataPacketType.EOD, ObjectType.LIST_FILEMETA));
        System.out.println("Sent first sync.");
    }
}
