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
    static FileSystem fs = new FileSystem("/Users/skreweverything/server/");

    static final int scanTimerInterval = 4000; // in milliseconds
    static volatile ConcurrentMap<String, AtomicInteger> sequenceNumberMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        RUDP server = new RUDP(5000, (socket) -> {
            System.out.println("New connection opened: " + socket.clientKey);
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
            }, 0, scanTimerInterval);
            while (true) {
                List<RUDPDataPacket> packets = socket.consumeAllPackets();
                System.out.println("Received " + packets.size() + " packets. -> " + packets);
                processList(packets, socket);
            }

        });
        server.debug = false;
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
        if (changedFiles.isEmpty()) return;

        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), changedFiles, ObjectType.LIST_FILEMETA));
        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.LIST_FILEMETA));

    }

    public static void processList(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        ObjectType type = dataPacketList.get(0).objectType;
        if (type == ObjectType.LIST_FILEMETA) {
            // In this we get a list of FileMeta data, and we need to decide which files we need from client.
            processReceivedFileMeta(dataPacketList, socket);
        }
        else if (type == ObjectType.LIST_FILEMETA_NEEDED) {
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

    //! NEED TO FIX IT! It's not working properly. Mainly,
    public static void processReceivedFileMeta(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        fs.reloadCachedFiles();
        List<FileMeta> fromOther = (List<FileMeta>) dataPacketList.get(0).data;
        List<FileMeta> fromUs = fs.getCachedFiles();
        List<FileMeta> filesWeWant = fromOther.stream().filter(f -> {
            for (FileMeta f2 : fromUs) {
                if (f.name.equals(f2.name)) {
                    return f.lastModifiedEpoch > f2.lastModifiedEpoch;
                }
            }
            return true;
        }).toList();

        System.out.println("FileMeta we got from client: " + fromOther);
        System.out.println("FileMeta we have: " + fromUs);
//        System.out.println("Files we need to send: " + newVersionFiles);

        if (filesWeWant.size() > 0) {
            System.out.println("We need this files: " + filesWeWant);
            sendListOfFilesNeeded(filesWeWant, socket);
        }
        else {
            System.out.println("We don't need any files from client.");
        }


    }

    public static void sendListOfFilesNeeded(List<FileMeta> files, RUDPSocket socket) throws IOException {
        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), files, ObjectType.LIST_FILEMETA_NEEDED));
        socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.LIST_FILEMETA_NEEDED));
    }

    public static void sendFiles(List<FileMeta> files, RUDPSocket socket) throws IOException {
        for (FileMeta f: files) {
            byte[] data = fs.readFromDisk(f.name);
            List<FileData> fileDataList = FileData.splitDataAndGetPackets(data, 4096, f);
            synchronized (sequenceNumberMap.get(socket.clientKey)) {
                if (fileDataList.isEmpty()) {
                    socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), new FileData(f, data), ObjectType.FILE_DATA));
                }
                for (FileData fd: fileDataList) {
                    socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), fd, ObjectType.FILE_DATA));
                }
                socket.send(new RUDPDataPacket(sequenceNumberMap.get(socket.clientKey).incrementAndGet(), RUDPDataPacketType.EOD, ObjectType.FILE_DATA));
            }
        }
    }

    public static void processReceivedFileData(List<RUDPDataPacket> dataPacketList, RUDPSocket socket) throws IOException {
        List<FileData> fileDataList = new ArrayList<>();
        for (RUDPDataPacket p: dataPacketList) {
            fileDataList.add((FileData) p.data);
        }
        FileData fileData = FileData.combinePackets(fileDataList);
        fs.writeToDisk(fileData.data, fileData.fileMeta);
    }
}
