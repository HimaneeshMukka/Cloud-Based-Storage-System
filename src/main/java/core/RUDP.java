package core;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RUDP implements Closeable {
    private InetAddress previousDataReceivedAddress;
    private int previousDataReceivedPort;
    private final DatagramSocket socket;

    private final ConcurrentMap<String, RUDPSocket> clients = new ConcurrentHashMap<>();
    private Boolean isListening = false;


    /**
     * Opens a UDP socket with random available port number
     * @throws SocketException If there is an error while opening the socket
     */
    public RUDP() throws SocketException {
        this.socket = new DatagramSocket();
    }

    /**
     * Open a UDP socket with specified port number
     * @param portNumber UDP socket port number
     * @throws SocketException If there is an error while opening the socket
     */
    public RUDP(int portNumber) throws SocketException {
        this.socket = new DatagramSocket(portNumber);
    }

    public void listen() {
        // Receiver

        new Thread(() -> {
            try {
                while (true) {
                    System.out.println("Waiting for data...");
                    byte[] buffer = new byte[100000000]; // 100Mb or 95MB
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                    this.socket.receive(datagramPacket);
                    RUDPDataPacket dataPacket = this.convertByteArrayToObject(datagramPacket.getData());

                    String clientKey = datagramPacket.getAddress().getHostAddress() + ":" + datagramPacket.getPort();

                    // If it is a new client, create a new socket for it
                    if(dataPacket.sequenceID == 0) {
                        System.out.println("Received handshake from client: " + clientKey);
                        this.clients.put(clientKey, new RUDPSocket(this.socket, datagramPacket.getAddress(), datagramPacket.getPort()));
                    }

                    if(this.clients.containsKey(clientKey)) {
                        System.out.println("Received data from client: " + clientKey + ": " + dataPacket);
                        this.clients.get(clientKey).receive(dataPacket);
                    }
                    else {
                        System.out.println("Received data from unknown client: " + clientKey + " - seqID: " + dataPacket.sequenceID);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void send(RUDPDataPacket dataPacket, InetAddress destinationAddress, int destinationPortNumber) throws IOException {
        final byte[] data = this.convertObjectToByteArray(dataPacket);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destinationAddress, destinationPortNumber);
        this.socket.send(datagramPacket);
    }

    public boolean send(RUDPDataPacket dataPacket) throws IOException {
//        assert this.previousDataReceivedAddress != null;
        if(this.previousDataReceivedAddress == null) {
            return false;
        }
        this.send(dataPacket, this.previousDataReceivedAddress, this.previousDataReceivedPort);
        return true;
    }

    public RUDPDataPacket receive() throws IOException, ClassNotFoundException {
        byte[] buffer = new byte[100000000]; // 100Mb or 95MB
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        this.socket.receive(datagramPacket);
        this.previousDataReceivedAddress = datagramPacket.getAddress();
        this.previousDataReceivedPort = datagramPacket.getPort();
        return this.convertByteArrayToObject(buffer);
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


    @Override
    public void close() {
        this.socket.close();
    }
}
