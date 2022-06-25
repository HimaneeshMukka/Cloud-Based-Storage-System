package core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class RUDPPacketSenderManager {
    private int totalPackets;
    private final ConcurrentMap<Integer, RUDPDataPacket> waitingForAck;
    private final ConcurrentSkipListMap<Long, Integer> timeStampsToID;
    private final ConcurrentMap<Integer, Long> idsToTimeStamps;
    private final ConcurrentLinkedDeque<RUDPDataPacket> packetList;

    public RUDPPacketSenderManager() {
        this.totalPackets = 0;
        this.waitingForAck = new ConcurrentHashMap<>();
        this.idsToTimeStamps = new ConcurrentHashMap<>();
        this.timeStampsToID = new ConcurrentSkipListMap<>();
        this.packetList = new ConcurrentLinkedDeque<>();
    }

    /**
     * Add packets to the sending queue.
     * @param packet data packet to send
     */
    public void addPacket(RUDPDataPacket packet) {
        packetList.offer(packet);
        totalPackets++;
    }

    /**
     * After receiving a packet's acknowledgement, update it
     * @param sequenceID data packet's sequence ID
     */
    public void gotAckFromReceiver(int sequenceID) {
        this.waitingForAck.remove(sequenceID);
        this.timeStampsToID.remove(this.idsToTimeStamps.get(sequenceID));
        this.idsToTimeStamps.remove(sequenceID);
    }

    /**
     * Gets a data packet from the list to send to the client.
     * @return gets next packet to send
     */
    public RUDPDataPacket getPacketToSend() {
        if (!this.waitingForAck.isEmpty()) {
            Long highestTimestamp = this.timeStampsToID.lastKey();
            if (System.currentTimeMillis() - highestTimestamp > 20) {
                int sequenceID = this.timeStampsToID.get(highestTimestamp);
                RUDPDataPacket dataPacket = this.waitingForAck.get(sequenceID);
                this.updateTimeStamp(dataPacket);
                return dataPacket;
            }
        }

        if (!packetList.isEmpty()) {
            RUDPDataPacket dataPacket = this.packetList.poll();
            this.updateTimeStamp(dataPacket);
            return dataPacket;
        }

        if (!this.waitingForAck.isEmpty()) {
            Long highestTimestamp = this.timeStampsToID.lastKey();
            int sequenceID = this.timeStampsToID.get(highestTimestamp);
            RUDPDataPacket dataPacket = this.waitingForAck.get(sequenceID);
            this.updateTimeStamp(dataPacket);
            return dataPacket;
        }
        return new RUDPDataPacket(totalPackets, null);
    }

    /**
     * Once the packet is taken out from the list or map, we need to update the timestamp
     * @param dataPacket data packet whose timestamp is to be updated
     */
    private void updateTimeStamp(RUDPDataPacket dataPacket) {
        int sequenceID = dataPacket.sequenceID;

        if(!this.waitingForAck.containsKey(sequenceID))
            this.waitingForAck.put(sequenceID, dataPacket);

        long currentTimestamp = System.currentTimeMillis();
        this.timeStampsToID.put(currentTimestamp, sequenceID);
        this.idsToTimeStamps.put(sequenceID, currentTimestamp);
    }
}