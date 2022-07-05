import core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class Server {
    static FileSystem fs = new FileSystem("/Users/skreweverything/server/");
    static volatile ConcurrentMap<String, AtomicInteger> sequenceNumberMap = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        RUDP server = new RUDP(5000, (socket) -> {
            System.out.println("New connection opened: " + socket.clientKey);
            sequenceNumberMap.put(socket.clientKey, new AtomicInteger(-1));
            while(true) {
                List<RUDPDataPacket> packets = socket.consumeAllPackets();
                System.out.println("Received " + packets.size() + " packets. -> " + packets);
                processList(packets, socket);
            }

        });
        server.debug = false;
        server.listen();

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

    public static void processList(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        ObjectType type = dataPacketList.get(0).objectType;
        if(type == ObjectType.LIST_FILEMETA) {
            // In this we get a list of FileMeta data, and we need to decide which files we need from client.
            processFileMeta(dataPacketList, socket);
        }
        else {
            System.out.println("Unknown object type: " + type + ", Type: " + dataPacketList.get(0).type);
        }
    }

    public static void processFileMeta(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        fs.reloadCachedFiles();
        List<FileMeta> fromOther = (List<FileMeta>) dataPacketList.get(0).data;
        List<FileMeta> newVersionFiles = fs.getNewVersionFiles(fromOther);
        List<FileMeta> filesWeNeedFromClient = fromOther.stream().filter(f -> {
            for(FileMeta fm : newVersionFiles) {
                if(fm.name.equals(f.name)) {
                    return false;
                }
            }
            return true;
        }).toList();

        System.out.println("FileMeta we got from client: " + fromOther);
        System.out.println("FileMeta we need from client: " + filesWeNeedFromClient);
        System.out.println("FileMeta we have: " + fs.getCachedFiles());
        System.out.println("Files we need to send: " + newVersionFiles);

        if(filesWeNeedFromClient.size() > 0) {
            System.out.println("We need to request: " + filesWeNeedFromClient);
            socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), filesWeNeedFromClient, ObjectType.LIST_FILEMETA_NEEDED));
            socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.LIST_FILEMETA_NEEDED));
        }
        else {
            System.out.println("We don't need any files from client.");
        }

        if(newVersionFiles.size() > 0) {
            System.out.println("We need to send: " + newVersionFiles);
            sendFiles(newVersionFiles, socket);
        }
        else {
            System.out.println("We don't need to send any files to client.");
        }


    }

    public static void sendFiles(List<FileMeta> files, RUDPSocket socket) throws IOException {
        for(FileMeta f : files) {
            byte[] data = fs.readFromDisk(f.name);
            List<FileData> fileDataList = FileData.splitDataAndGetPackets(data, 4096, f);
            synchronized (sequenceNumberMap.get(socket.clientKey)) {
                if(fileDataList.isEmpty()){
                    socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), new FileData(f, null), ObjectType.FILE_DATA));
                }
                for(FileData fd : fileDataList) {
                    socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), fd, ObjectType.FILE_DATA));
                }
                socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.FILE_DATA));
            }
        }
    }
}
