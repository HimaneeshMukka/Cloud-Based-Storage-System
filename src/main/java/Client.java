import core.FileSystem;
import core.RUDP;
import core.RUDPDataPacket;
import core.RUDPDataPacketType;
import core.RUDPSocket;

import java.io.*;
import java.net.*;

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
        socket.send(new RUDPDataPacket(0, fs.getCachedFiles()));
        socket.send(new RUDPDataPacket(1, RUDPDataPacketType.EOD));
        socket.send(new RUDPDataPacket(3, fs.getCachedFiles()));
        socket.send(new RUDPDataPacket(2, fs.getCachedFiles()));
        sleep(10000);
        socket.send(new RUDPDataPacket(5, RUDPDataPacketType.EOD));
        socket.send(new RUDPDataPacket(4, fs.getCachedFiles()));
        sleep(10000);
        socket.send(new RUDPDataPacket(6, fs.getCachedFiles()));
        socket.send(new RUDPDataPacket(7, RUDPDataPacketType.EOD));
        System.out.println("Sent first sync.");
    }
}
