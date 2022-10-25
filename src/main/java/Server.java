import core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


public class Server {
    static FileSync fileSync;
    static final int scanTimerInterval = 400; // in milliseconds

    static ConcurrentMap<String, RUDPSocket> clientSockets = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        fileSync = new FileSync(args[0]);
        RUDP server = new RUDP(5000, (socket) -> {
            if(!clientSockets.containsKey(socket.clientKey)) {
                clientSockets.put(socket.clientKey, socket);
            }

            System.out.println("New connection opened: " + socket.clientKey);
            fileSync.sequenceNumberMap.put(socket.clientKey, new AtomicInteger(-1));
            fileSync.sendCachedFileMeta(socket);


            while (true) {
                List<RUDPDataPacket> packets = socket.consumeAllPackets();
//                System.out.println("Received " + packets.size() + " packets. -> " + packets);
                fileSync.processList(packets, socket);
            }

        });
        server.debug = false;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<FileMeta> changedFiles = new ArrayList<>();
                    changedFiles.addAll(fileSync.fs.getAddedFiles());
                    changedFiles.addAll(fileSync.fs.getModifiedFiles());
                    changedFiles.addAll(fileSync.fs.getDeletedFiles());
//                    System.out.println("Changed files: " + changedFiles);
                    fileSync.fs.reloadCachedFiles();
                    if(changedFiles.isEmpty()) return;

                    for(RUDPSocket socket : clientSockets.values()) {
                        fileSync.sendChangelistFileMeta(socket, changedFiles);
                    }

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, scanTimerInterval);
    }
}
