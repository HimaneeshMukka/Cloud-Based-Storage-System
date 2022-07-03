package core;

import java.io.Serializable;

public class RUDPDataPacket implements Comparable<RUDPDataPacket>, Serializable {
    int sequenceID;
    Object data;

    public RUDPDataPacketType type;

    public RUDPDataPacket(int sequenceID, RUDPDataPacketType type) {
        this.sequenceID = sequenceID;
        this.type = type;
    }

    public RUDPDataPacket(int sequenceID, Object data) {
        this.data = data;
        this.sequenceID = sequenceID;
        this.type = RUDPDataPacketType.DATA;
    }

    @Override
    public int compareTo(RUDPDataPacket o) {
        return Integer.compare(this.sequenceID, o.sequenceID);
    }

    public String toString() {
        return "{ Type: " + this.type + ", SeqID: " + this.sequenceID + ", Data: " + this.data + "}";
    }
}

