package core;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class RUDPSocket {
    public final InetAddress destinationAddress;
    public final int destinationPortNumber;
    private final DatagramSocket socket;
    private final RUDPPacketSenderManager packetSenderManager;
    private boolean isPacketSenderManagerRunning;
    private final RUDPPacketReceiverManager packetReceiverManager;

    public RUDPSocket(DatagramSocket socket, InetAddress destinationAddress, int destinationPortNumber) {
        this.socket = socket;
        this.destinationAddress = destinationAddress;
        this.destinationPortNumber = destinationPortNumber;
        this.packetSenderManager = new RUDPPacketSenderManager();
        this.packetReceiverManager = new RUDPPacketReceiverManager();
        this.isPacketSenderManagerRunning = false;
    }

    public synchronized void send(RUDPDataPacket dataPacket) throws IOException {
        packetSenderManager.addPacket(dataPacket);
        // Start the sender manager for the destination if it doesn't exist
        this.startSenderManagerTimer();

        final byte[] data = this.convertObjectToByteArray(dataPacket);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destinationAddress, destinationPortNumber);
        this.socket.send(datagramPacket);
    }

    private synchronized void startSenderManagerTimer() {
        if (!this.isPacketSenderManagerRunning) {
            this.isPacketSenderManagerRunning = true;
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                               @Override
                               public void run() {
                                   RUDPDataPacket dataPacket = packetSenderManager.getPacketToSend();

                                   try {
                                       System.out.println("***********************");
                                       System.out.println("Sending data: " + dataPacket);
                                       System.out.println("To: " + destinationAddress + ":" + destinationPortNumber);
                                       System.out.println("***********************");
                                       RUDPSocket.this.send(dataPacket);
                                   } catch (IOException e) {
                                       e.printStackTrace();
                                   }
                               }
                           }
                    , 0, 10);
        }
    }

    public synchronized void receive(RUDPDataPacket dataPacket) throws IOException, ClassNotFoundException {

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
