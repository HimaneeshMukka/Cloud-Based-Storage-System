package core;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

public class RUDPSocket {
    public final InetAddress destinationAddress;
    public final int destinationPortNumber;
    private final DatagramSocket socket;
    private final RUDPPacketSenderManager packetSenderManager;
    private boolean isPacketSenderManagerRunning;
    private final RUDPPacketReceiverManager packetReceiverManager;
    public final String clientKey;

    public boolean debug = false;

    public RUDPSocket(DatagramSocket socket, InetAddress destinationAddress, int destinationPortNumber) {
        this.socket = socket;
        this.destinationAddress = destinationAddress;
        this.destinationPortNumber = destinationPortNumber;
        this.packetSenderManager = new RUDPPacketSenderManager();
        this.packetReceiverManager = new RUDPPacketReceiverManager();
        this.clientKey = destinationAddress.getHostAddress() + ":" + destinationPortNumber;
        this.isPacketSenderManagerRunning = false;
    }

    public synchronized void send(RUDPDataPacket dataPacket) throws IOException {
        packetSenderManager.addPacket(dataPacket);
        // Start the sender manager for the destination if it doesn't exist
        if (!this.isPacketSenderManagerRunning) this.startSenderManagerTimer();

        final byte[] data = this.convertObjectToByteArray(dataPacket);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destinationAddress, destinationPortNumber);
        this.socket.send(datagramPacket);
    }

    public void retransmitPacket(RUDPDataPacket dataPacket) throws IOException {
        final byte[] data = this.convertObjectToByteArray(dataPacket);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destinationAddress, destinationPortNumber);
        this.socket.send(datagramPacket);
    }

    public void retransmitPacket(int sequenceID) throws IOException {
        this.retransmitPacket(this.packetSenderManager.getPacket(sequenceID));
    }

    public void sendAck(RUDPDataPacket dataPacket) throws IOException {
        this.retransmitPacket(dataPacket);
    }

    /**
     * The protocol we are doing is, if we don't get a `ACK` from the server, we will resend the packet.
     * The receiver will not request a new packet.
     * <p>
     * It is the manager's responsibility to check if the packet has been received.
     */
    private void startSenderManagerTimer() {
        if(debug) System.out.println("Starting packet sender manager");
        this.isPacketSenderManagerRunning = true;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
                           @Override
                           public synchronized void run() {
                               RUDPDataPacket dataPacket = packetSenderManager.getPacketToSend();
                               if (dataPacket == null) {
                                   if(debug) System.out.println("No more packets to send: " + RUDPSocket.this.destinationAddress + ":" + RUDPSocket.this.destinationPortNumber);
                                   isPacketSenderManagerRunning = false;
                                   timer.cancel();
                                   return;
                               }

                               try {
                                   if(debug)System.out.println("***********************\n" + "ReSending data: " + dataPacket + "\n To: " + RUDPSocket.this.destinationAddress + ":" + RUDPSocket.this.destinationPortNumber + "\n***********************");
                                   RUDPSocket.this.retransmitPacket(dataPacket);
                               } catch (IOException e) {
                                   e.printStackTrace();
                               }
                           }
                       }
                , 0, 10);

    }

    /**
     * If the dataPacket is `ACK`, forward that to the sender manager.
     * If the dataPacket is `DATA` or `EOD`, forward that to the receiver manager.
     *
     * @param dataPacket Data packet to be received
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void receive(RUDPDataPacket dataPacket) throws IOException, ClassNotFoundException {
        // If the received packet is ACK, remove it from the sender manager
        if (dataPacket.type == RUDPDataPacketType.ACK || dataPacket.type == RUDPDataPacketType.HANDSHAKE_ACK) {
            this.packetSenderManager.gotAckFromReceiver(dataPacket.sequenceID);
        }
        else if (dataPacket.type == RUDPDataPacketType.DATA || dataPacket.type == RUDPDataPacketType.EOD) {
            packetReceiverManager.addPacket(dataPacket);
            // Send an ACK packet to the sender
            this.sendAck(new RUDPDataPacket(dataPacket.sequenceID, RUDPDataPacketType.ACK));
        }
        else if (dataPacket.type == RUDPDataPacketType.NAK) {
            this.retransmitPacket(dataPacket.sequenceID);
        }
        else {
            System.out.println("Unknown packet type: " + dataPacket.type);
        }
    }

    public RUDPDataPacket consume() throws InterruptedException {
        return packetReceiverManager.consumePacket(() -> {
            int nextSequence = RUDPSocket.this.packetReceiverManager.getNextMissingSequenceID();
            RUDPDataPacket dataPacket = new RUDPDataPacket(nextSequence, RUDPDataPacketType.NAK);
            try {
                RUDPSocket.this.sendAck(dataPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }, 5000);
    }

    /**
     * Get a list of packets that belong to same data stream.
     * @return
     * @throws InterruptedException
     */
    public List<RUDPDataPacket> consumeAllPackets() throws InterruptedException {
        RUDPDataPacket dataPacket = null;
        List<RUDPDataPacket> dataPacketList = new ArrayList<>();
        while (true) {
            dataPacket = this.consume();
//                    System.out.println("Consumed data packet: " + dataPacket);
            if (dataPacket == null) {
                sleep(100);
                continue;
            }
            if (dataPacket.type == RUDPDataPacketType.EOD) {
                break;
            }
            dataPacketList.add(dataPacket);
        }

//        if (dataPacketList.size() > 0)
            return dataPacketList;
    }


    private byte[] convertObjectToByteArray(RUDPDataPacket dataPacket) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(dataPacket);
        return baos.toByteArray();
    }

    private RUDPDataPacket convertByteArrayToObject(byte[] buffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
        Object o = is.readObject();
        return (RUDPDataPacket) o;
    }


}
