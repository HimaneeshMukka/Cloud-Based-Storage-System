package core;

import java.util.Objects;
import java.util.concurrent.*;

public class RUDPPacketReceiverManager {
    private final ConcurrentSkipListSet<RUDPDataPacket> packetsReceived;
    private final ConcurrentSkipListSet<Integer> packetsReceivedSeqID;
    private final BlockingQueue<RUDPDataPacket> consumerQueue;

    private volatile boolean endOfData;
    private volatile int endOfDataSeqID = -1;

    public RUDPPacketReceiverManager() {
        this.packetsReceived = new ConcurrentSkipListSet<>();
        this.packetsReceivedSeqID = new ConcurrentSkipListSet<>();
        this.consumerQueue = new LinkedBlockingQueue<>();
        this.endOfData = false;
    }

    /**
     * Add packet that was received from the server.
     *
     * @param packet data packet to add to the list.
     */
    public synchronized void addPacket(RUDPDataPacket packet) {
        if(packet.type == RUDPDataPacketType.EOD) {
            this.endOfData = true;
            this.endOfDataSeqID = packet.sequenceID;
        }

        if(!this.packetsReceivedSeqID.contains(packet.sequenceID)) {
            if(this.endOfData && this.endOfDataSeqID < packet.sequenceID) {
                this.endOfData = false;
                this.endOfDataSeqID = -1;
            }
            this.packetsReceived.add(packet);
            this.packetsReceivedSeqID.add(packet.sequenceID);
            this.updateConsumerQueue();
        }

    }

    private void updateConsumerQueue(){
        int pollTill = this.getNextMissingSequenceID();

        if(pollTill == -2) return;

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
    public synchronized int getNextMissingSequenceID() {
        int prev = -1;
        for(Integer sequenceID: this.packetsReceivedSeqID){
            if(sequenceID - prev != 1)
                return ++prev;
            prev = sequenceID;
        }
        ++prev;

//        System.out.println("Missing packet: " + prev + " EOD: " + this.endOfDataSeqID);
        if(this.endOfDataSeqID != -1 && prev >= this.endOfDataSeqID) return -2;

        return prev;
    }

    public RUDPDataPacket consumePacket(TimeoutCallback timeoutCallback, long timeoutInMS) throws InterruptedException {
        this.updateConsumerQueue();
        while(true){
            // We got all the packets. Return null packet.
            if (this.getNextMissingSequenceID() == -2) return null;

            RUDPDataPacket dataPacket = this.consumerQueue.poll(timeoutInMS, TimeUnit.MILLISECONDS);
            if (dataPacket == null && timeoutCallback != null && !this.packetsReceivedSeqID.isEmpty()) timeoutCallback.call();
            if(dataPacket != null) return dataPacket;
        }
    }


}
