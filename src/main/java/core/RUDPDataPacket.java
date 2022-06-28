package core;

import java.io.Serializable;

public class RUDPDataPacket implements Comparable<RUDPDataPacket>, Serializable {
    int sequenceID;
    Object data;

    public RUDPDataPacket(int sequenceID, Object data) {
        this.data = data;
        this.sequenceID = sequenceID;
    }

    @Override
    public int compareTo(RUDPDataPacket o) {
        return Integer.compare(this.sequenceID, o.sequenceID);
    }

    public String toString() {
        return "{\nSeqID: " + this.sequenceID + "\nData: " + this.data + "}\n";
    }
}
