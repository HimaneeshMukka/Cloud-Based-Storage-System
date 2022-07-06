import core.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class Client {
    static FileSystem fs = new FileSystem("/Users/skreweverything/client1/");
    static final int scanTimerInterval = 4000; // in milliseconds
    static volatile ConcurrentMap<String, AtomicInteger> sequenceNumberMap = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        RUDP client = new RUDP();
        RUDPSocket socket = client.connect(InetAddress.getLocalHost(), 5000);
        sequenceNumberMap.put(socket.clientKey, new AtomicInteger(-1));
        sendCachedFileMeta(socket);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendChangelistFileMeta(socket);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, scanTimerInterval);
        while(true) {
            List<RUDPDataPacket> packets = socket.consumeAllPackets();
            System.out.println("Received " + packets.size() + " packets. -> " + packets);
            processList(packets, socket);
        }
//        data = client.receive();
//        System.out.println("Received data from server: " + data);
    }

    public static void sendCachedFileMeta(RUDPSocket socket) throws IOException, InterruptedException {
//        System.out.println("Sending first sync...");
        fs.reloadCachedFiles();
        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), fs.getCachedFiles(), ObjectType.LIST_FILEMETA));
        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.LIST_FILEMETA));
        fs.reloadCachedFiles();
//        System.out.println("Sent first sync.");
    }

    public static void sendChangelistFileMeta(RUDPSocket socket) throws IOException, InterruptedException {
        List<FileMeta> changedFiles = new ArrayList<>();
        changedFiles.addAll(fs.getAddedFiles());
        changedFiles.addAll(fs.getModifiedFiles());
        changedFiles.addAll(fs.getDeletedFiles());
        fs.reloadCachedFiles();
        if(changedFiles.isEmpty()) return;

        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), changedFiles, ObjectType.LIST_FILEMETA));
        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.LIST_FILEMETA));

    }
    public static void processList(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        ObjectType type = dataPacketList.get(0).objectType;
        if(type == ObjectType.LIST_FILEMETA) {
            // In this we get a list of FileMeta data, and we need to decide which files we need from client.
            processReceivedFileMeta(dataPacketList, socket);
        }
        else if (type ==ObjectType.LIST_FILEMETA_NEEDED) {
            sendFiles((List<FileMeta>) dataPacketList.get(0).data, socket);
        }
        else if (type == ObjectType.FILE_DATA) {
            // In this we get a file data, and we need to save it to the file system.
            processReceivedFileData(dataPacketList, socket);
        }
        else {
            System.out.println("Unknown object type: " + type + ", Type: " + dataPacketList.get(0).type);
        }
    }

    public static void processReceivedFileMeta(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        fs.reloadCachedFiles();
        List<FileMeta> fromOther = (List<FileMeta>) dataPacketList.get(0).data;
        List<FileMeta> newVersionFiles = fs.getNewVersionFiles(fromOther);

        System.out.println("FileMeta we got from client: " + fromOther);
        System.out.println("FileMeta we have: " + fs.getCachedFiles());
//        System.out.println("Files we need to send: " + newVersionFiles);

        if(newVersionFiles.size() > 0) {
            System.out.println("We need to send: " + newVersionFiles);
            sendFiles(newVersionFiles, socket);
        }
        else {
//            System.out.println("We don't need to send any files to client.");
        }


    }

    public static void sendFiles(List<FileMeta> files, RUDPSocket socket) throws IOException {
        for(FileMeta f : files) {
            byte[] data = fs.readFromDisk(f.name);
            List<FileData> fileDataList = FileData.splitDataAndGetPackets(data, 4096, f);
            synchronized (sequenceNumberMap.get(socket.clientKey)) {
                if(fileDataList.isEmpty()){
                    socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), new FileData(f, data), ObjectType.FILE_DATA));
                }
                for(FileData fd : fileDataList) {
                    socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), fd, ObjectType.FILE_DATA));
                }
                socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.FILE_DATA));
            }
        }
    }

    public static void processReceivedFileData(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        List<FileData> fileDataList = new ArrayList<>();
        for(RUDPDataPacket p : dataPacketList) {
            fileDataList.add((FileData) p.data);
        }
        FileData fileData = FileData.combinePackets(fileDataList);
        fs.writeToDisk(fileData.data, fileData.fileMeta);
    }
}
