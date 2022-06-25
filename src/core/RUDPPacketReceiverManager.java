package core;

import java.util.concurrent.*;

public class RUDPPacketReceiverManager {
    private final ConcurrentSkipListSet<RUDPDataPacket> packetsReceived;

    public RUDPPacketReceiverManager() {
        this.packetsReceived = new ConcurrentSkipListSet<>();
    }

    /**
     * Add packet that was received from the server.
     * @param packet data packet to add to the list.
     * @return sequence id of the packet to send as an acknowledgement id back to sender.
     */
    public int addPacket(RUDPDataPacket packet) {
        this.packetsReceived.add(packet);
        return packet.sequenceID;
    }

    /**
     * Returns a next sequence ID that is missing.
     *
     * The sequence ID starts from `0`
     *
     * If there are no sequence IDs left, it will return `-1`.
     * @return next sequence ID of the packet
     */
    public int getNextMissingSequenceID() {
        int prev = -1;
        for(RUDPDataPacket dataPacket: packetsReceived){
            if(dataPacket.sequenceID - prev != 1)
                return ++prev;
            prev = dataPacket.sequenceID;
        }

        if(this.packetsReceived.last().data == null)
            return -1;

        return ++prev;
    }

}
