package core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RUDPPacketSenderManagerTest {
    // Run before each test
    RUDPPacketSenderManager senderManager;

    @BeforeEach
    public void beforeEach() {
        senderManager = new RUDPPacketSenderManager();
    }

    @Test
    void addPacket() {
        RUDPDataPacket packet = new RUDPDataPacket(0, RUDPDataPacketType.DATA, ObjectType.FILE_DATA);
        senderManager.addPacket(packet);
        assertEquals(packet, senderManager.getPacketToSend());
        assertEquals(1, senderManager.totalPacketsWaitingForAcknowledgement());
    }

    @Test
    void testWaitingForAcknowledgement() {
        for (int i = 0; i < 10; i++) {
            senderManager.addPacket(new RUDPDataPacket(i, RUDPDataPacketType.DATA));
        }

        assertEquals(0, senderManager.totalPacketsWaitingForAcknowledgement());

        for(int i = 0; i < 10; i++)
            senderManager.getPacketToSend();
        assertEquals(10, senderManager.totalPacketsWaitingForAcknowledgement());
    }

    @Test
    void testGotAcks() {
        senderManager.addPacket(new RUDPDataPacket(0, RUDPDataPacketType.DATA));

        assertEquals(0, senderManager.totalPacketsWaitingForAcknowledgement());
        senderManager.getPacketToSend();
        assertEquals(1, senderManager.totalPacketsWaitingForAcknowledgement());
        senderManager.gotAckFromReceiver(0);
        assertEquals(0, senderManager.totalPacketsWaitingForAcknowledgement());
    }

    @Test
    void testGetPacket() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            senderManager.addPacket(new RUDPDataPacket(i, RUDPDataPacketType.DATA));
        }

        for (int i = 0; i < 10; i++) {
            senderManager.getPacketToSend();
        }

        assertEquals(10, senderManager.totalPacketsWaitingForAcknowledgement());
        sleep(100);

        for (int i = 0; i < 10; i++) {
            senderManager.getPacketToSend();
        }

        for(int i = 0; i < 10; i++) {
            senderManager.gotAckFromReceiver(i);
        }

        assertEquals(0, senderManager.totalPacketsWaitingForAcknowledgement());

        assertNull(senderManager.getPacketToSend());
    }

    @Test
    void testGetPacketWithSeqID() {
        senderManager.addPacket(new RUDPDataPacket(0, RUDPDataPacketType.DATA));
        senderManager.addPacket(new RUDPDataPacket(1, RUDPDataPacketType.DATA));

        assertEquals(0, senderManager.getPacketToSend().sequenceID);
        assertEquals(0, senderManager.getPacket(0).sequenceID);
        assertEquals(1, senderManager.getPacket(1).sequenceID);
        assertNull(senderManager.getPacket(2));

    }
}
