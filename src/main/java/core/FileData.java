package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileData implements Serializable {
    public final FileMeta fileMeta;
    public final byte[] data;

    public FileData(FileMeta fileMeta, byte[] data) {
        this.fileMeta = fileMeta;
        this.data = data;
    }

    public static List<FileData> splitDataAndGetPackets(byte[] data, int packetSize, FileMeta fileMeta) {
        List<FileData> packets = new ArrayList<>();
        int numPackets = (int) Math.ceil((double) data.length / packetSize);
        for (int i = 0; i < numPackets; i++) {
            int start = i * packetSize;
            int end = (i + 1) * packetSize;
            if (end > data.length) {
                end = data.length;
            }
            byte[] packetData = new byte[end - start];
            System.arraycopy(data, start, packetData, 0, end - start);
            packets.add(new FileData(fileMeta, packetData));
        }
        return packets;
    }
}
