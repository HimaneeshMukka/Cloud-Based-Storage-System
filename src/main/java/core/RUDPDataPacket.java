package core;

import java.io.Serializable;

public class RUDPDataPacket implements Comparable<RUDPDataPacket>, Serializable {
    public int sequenceID;
    public Object data;

    public RUDPDataPacketType type;
    public ObjectType objectType;

    public RUDPDataPacket(int sequenceID, RUDPDataPacketType type, ObjectType objectType) {
        this.sequenceID = sequenceID;
        this.objectType = objectType;
        this.type = type;
    }

    public RUDPDataPacket(int sequenceID, RUDPDataPacketType type) {
        this.sequenceID = sequenceID;
        this.type = type;
    }

    public RUDPDataPacket(int sequenceID, Object data, ObjectType objectType) {
        this.data = data;
        this.sequenceID = sequenceID;
        this.objectType = objectType;
        this.type = RUDPDataPacketType.DATA;
    }

    @Override
    public int compareTo(RUDPDataPacket o) {
        return Integer.compare(this.sequenceID, o.sequenceID);
    }

    public String toString() {
        return "{ Type: " + this.type + ", SeqID: " + this.sequenceID + ", ObjectType: " + this.objectType + ", Data: " + this.data + "}";
    }
}

