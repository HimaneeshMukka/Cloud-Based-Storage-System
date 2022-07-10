package core;

import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.junit.jupiter.api.Assertions.*;

public class RUDPPacketReceiverManagerTest {

    RUDPPacketReceiverManager receiverManager;

    // junit 5 run before each test
    @BeforeEach
    public void beforeEach() {
        System.out.println("Before each test");
        receiverManager = new RUDPPacketReceiverManager();
    }


    @Test
    void testMissingPacket() {
        assertEquals(0, receiverManager.getNextMissingSequenceID());
        for(int i = 0; i <= 10; i++) {
            receiverManager.addPacket(new RUDPDataPacket(i, RUDPDataPacketType.DATA));
        }

        receiverManager.addPacket(new RUDPDataPacket(12, RUDPDataPacketType.DATA));
        int missingPacket = receiverManager.getNextMissingSequenceID();
        assertEquals(11, missingPacket);
    }

    @Test
    void testMissingPacketAfterEOD() {
        for(int i = 0; i <= 10; i++) {
            receiverManager.addPacket(new RUDPDataPacket(i, RUDPDataPacketType.DATA));
        }

        receiverManager.addPacket(new RUDPDataPacket(11, RUDPDataPacketType.EOD));
        int missingPacket = receiverManager.getNextMissingSequenceID();
        assertEquals(-2, missingPacket);
    }

    @Test
    void testMissingPacketAfterEODWithNewPacket() {
        for(int i = 0; i <= 10; i++) {
            receiverManager.addPacket(new RUDPDataPacket(i, RUDPDataPacketType.DATA));
        }

        receiverManager.addPacket(new RUDPDataPacket(11, RUDPDataPacketType.EOD));
        int missingPacket = receiverManager.getNextMissingSequenceID();
        assertEquals(-2, missingPacket);
        receiverManager.addPacket(new RUDPDataPacket(12, RUDPDataPacketType.DATA));
        missingPacket = receiverManager.getNextMissingSequenceID();
        assertEquals(13, missingPacket);
    }

    @Test
    void consumePacketWithoutMissingPacket() throws InterruptedException {
        List<RUDPDataPacket> expectedPacketList = new ArrayList<>();
        for(int i = 0; i <= 10; i++) {
            RUDPDataPacket packet = new RUDPDataPacket(i, RUDPDataPacketType.DATA);
            receiverManager.addPacket(packet);
            expectedPacketList.add(packet);
        }
        RUDPDataPacket packet = new RUDPDataPacket(11, RUDPDataPacketType.EOD);
        receiverManager.addPacket(packet);
        expectedPacketList.add(packet);
        RUDPDataPacket s;
        List<RUDPDataPacket> actualPacketList = new ArrayList<>();
        while((s = receiverManager.consumePacket(null, 1000)) != null){
            actualPacketList.add(s);
        }

        assertEquals(expectedPacketList, actualPacketList);
    }

    @Test
    void consumePacketWithMissingPacketAndAvoidBlocking() throws InterruptedException {
        List<RUDPDataPacket> expectedPacketList = new ArrayList<>();
        for(int i = 0; i <= 10; i++) {
            RUDPDataPacket packet = new RUDPDataPacket(i, RUDPDataPacketType.DATA);
            receiverManager.addPacket(packet);
            expectedPacketList.add(packet);
        }

        receiverManager.addPacket(new RUDPDataPacket(12, RUDPDataPacketType.DATA));
        expectedPacketList.add(new RUDPDataPacket(12, RUDPDataPacketType.DATA));
        RUDPDataPacket packet = new RUDPDataPacket(13, RUDPDataPacketType.EOD);
        receiverManager.addPacket(packet);
        expectedPacketList.add(packet);

        final RUDPDataPacket[] s = new RUDPDataPacket[1];
        List<RUDPDataPacket> actualPacketList = new ArrayList<>();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                receiverManager.addPacket(new RUDPDataPacket(11, RUDPDataPacketType.DATA));
                expectedPacketList.add(11, new RUDPDataPacket(11, RUDPDataPacketType.DATA));
            }
        }, 1500);

        assertTimeout(Duration.ofMillis(3000), () -> {
            while((s[0] = receiverManager.consumePacket(null, 1000)) != null){
                actualPacketList.add(s[0]);
            }
        });

        assertEquals(expectedPacketList.toString(), actualPacketList.toString());

    }

}
