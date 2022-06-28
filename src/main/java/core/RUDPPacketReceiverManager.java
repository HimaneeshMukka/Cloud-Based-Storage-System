package core;

import java.util.Objects;
import java.util.concurrent.*;

public class RUDPPacketReceiverManager {
    private final ConcurrentSkipListSet<RUDPDataPacket> packetsReceived;
    private final ConcurrentSkipListSet<Integer> packetsReceivedSeqID;
    private final BlockingQueue<RUDPDataPacket> consumerQueue;

    private boolean endOfData;

    public RUDPPacketReceiverManager() {
        this.packetsReceived = new ConcurrentSkipListSet<>();
        this.packetsReceivedSeqID = new ConcurrentSkipListSet<>();
        this.consumerQueue = new LinkedBlockingQueue<>();
        this.endOfData = false;
    }

    /**
     * Add packet that was received from the server.
     * @param packet data packet to add to the list.
     * @return sequence id of the packet to send as an acknowledgement id back to sender.
     */
    public int addPacket(RUDPDataPacket packet) {
        if(packet.data == null) this.endOfData = true;
        this.packetsReceivedSeqID.add(packet.sequenceID);
        this.packetsReceived.add(packet);
        this.updateConsumerQueue();
        return packet.sequenceID;
    }

    private void updateConsumerQueue(){
        if(this.endOfData) return;

        int pollTill = this.getNextMissingSequenceID();
        while(!this.packetsReceived.isEmpty() && pollTill > this.packetsReceived.first().sequenceID)
            this.consumerQueue.offer(Objects.requireNonNull(this.packetsReceived.pollFirst()));
    }


    /**
     * Returns a next sequence ID that is missing.
     *
     * The sequence ID starts from `0`
     *
     * If there are no sequence IDs left, it will return `-2`.
     * @return next sequence ID of the packet
     */
    public int getNextMissingSequenceID() {
        if(this.endOfData) return -2;
        int prev = -1;
        for(Integer sequenceID: this.packetsReceivedSeqID){
            if(sequenceID - prev != 1)
                return ++prev;
            prev = sequenceID;
        }

        return ++prev;
    }

    public RUDPDataPacket consumePacket(TimeoutCallback timeoutCallback, long timeoutInMS) throws InterruptedException {
        this.updateConsumerQueue();
        while(true){
            if (this.endOfData) return null;
            RUDPDataPacket dataPacket = this.consumerQueue.poll(timeoutInMS, TimeUnit.MILLISECONDS);
            if (dataPacket == null)
                timeoutCallback.call();
            else return dataPacket;
        }
    }


}
