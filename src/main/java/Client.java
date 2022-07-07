import core.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class Client {
    static FileSystem fs;
    static final int scanTimerInterval = 40; // in milliseconds
    static FileSync fileSync;
    public static void main(String[] args) throws IOException, InterruptedException {
        fileSync = new FileSync(args[0]);

        RUDP client = new RUDP();
        RUDPSocket socket = client.connect(InetAddress.getLocalHost(), 5000);
        fileSync.sequenceNumberMap.put(socket.clientKey, new AtomicInteger(-1));
        fileSync.sendCachedFileMeta(socket);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    fileSync.sendChangelistFileMeta(socket);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, scanTimerInterval);

        while(true) {
            List<RUDPDataPacket> packets = socket.consumeAllPackets();
            System.out.println("Received " + packets.size() + " packets. -> " + packets);
            fileSync.processList(packets, socket);
        }
//        data = client.receive();
//        System.out.println("Received data from server: " + data);
    }
}
