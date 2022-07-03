package core;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Thread.sleep;

public class RUDP implements Closeable {
    private InetAddress previousDataReceivedAddress;
    private int previousDataReceivedPort;
    private final DatagramSocket socket;

    private final ConcurrentMap<String, RUDPSocket> clients = new ConcurrentHashMap<>();
    private final StringBuilder isListening = new StringBuilder("false");


    /**
     * Opens a UDP socket with random available port number
     *
     * @throws SocketException If there is an error while opening the socket
     */
    public RUDP() throws SocketException {
        this.socket = new DatagramSocket();
        this.listen();
    }

    /**
     * Open a UDP socket with specified port number
     *
     * @param portNumber UDP socket port number
     * @throws SocketException If there is an error while opening the socket
     */
    public RUDP(int portNumber) throws SocketException {
        this.socket = new DatagramSocket(portNumber);
        this.listen();
    }

    public synchronized void listen() {
        // Receiver

        // Idempotent operation (if already listening, do nothing)
        if (Boolean.parseBoolean(this.isListening.toString())) {
            System.out.println("Already listening");
            return;
        }
        this.isListening.setLength(0);
        this.isListening.append("true");
        System.out.println("Listening");
        new Thread(() -> {
            try {
                while (true) {
                    System.out.println("\n\nWaiting for data...");
                    byte[] buffer = new byte[100000000]; // 100Mb or 95MB
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                    this.socket.receive(datagramPacket);
                    RUDPDataPacket dataPacket = this.convertByteArrayToObject(datagramPacket.getData());

                    String clientKey = datagramPacket.getAddress().getHostAddress() + ":" + datagramPacket.getPort();

                    if(dataPacket == null) {
                        System.out.println("Received null packet from: " + clientKey);
                        continue;
                    }
                    switch (dataPacket.type) {
                        case HANDSHAKE:
                            System.out.println("***********************\nReceived handshake from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            RUDPSocket newClient = new RUDPSocket(this.socket, datagramPacket.getAddress(), datagramPacket.getPort());
                            this.clients.put(clientKey, newClient);
                            RUDPDataPacket handshakeACK = new RUDPDataPacket(dataPacket.sequenceID, RUDPDataPacketType.HANDSHAKE_ACK);
                            newClient.send(handshakeACK);
                            // Remove it from waitingForACKS map immediately after sending the ACK
                            newClient.receive(handshakeACK);
                            break;
                        case HANDSHAKE_ACK:
                            System.out.println("***********************\nReceived handshake_ack from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            // This means we have already created a client, and now we remove it from the waitingForACKS map
                            if (this.clients.containsKey(clientKey)) {
                                this.clients.get(clientKey).receive(dataPacket);
                            }
                            // This happens when we sent a HANDSHAKE packet to the server directly and now server has returned ACK.
                            else {
                                RUDPSocket newClient1 = new RUDPSocket(this.socket, datagramPacket.getAddress(), datagramPacket.getPort());
                                this.clients.put(clientKey, newClient1);
                            }
                            break;
                        case ACK:
                            System.out.println("***********************\nReceived ack from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            if (this.clients.containsKey(clientKey)) {
                                this.clients.get(clientKey).receive(dataPacket);
                            }
                            break;
                        case DATA:
                            System.out.println("***********************\nReceived data from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            if (this.clients.containsKey(clientKey)) {
                                this.clients.get(clientKey).receive(dataPacket);
                                break;
                            }
                        default:
                            System.out.println("***********************\nReceived unknown data/client from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            break;

                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                this.isListening.setLength(0);
                this.isListening.append("false");
                e.printStackTrace();
            }
        }).start();

    }

    public void send(RUDPDataPacket dataPacket, InetAddress destinationAddress, int destinationPortNumber) throws IOException {
        final byte[] data = this.convertObjectToByteArray(dataPacket);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destinationAddress, destinationPortNumber);
        this.socket.send(datagramPacket);
    }

    public RUDPSocket connect(InetAddress destinationAddress, int destinationPortNumber) {
        // Send a handshake packet to the server
        RUDPDataPacket handshake = new RUDPDataPacket(0, RUDPDataPacketType.HANDSHAKE);
        String clientKey = destinationAddress.getHostAddress() + ":" + destinationPortNumber;
        int numberOfAttempts = 1;
        while(!this.clients.containsKey(clientKey)) {
            try {
                this.send(handshake, destinationAddress, destinationPortNumber);
                System.out.println("Sent handshake to: " + clientKey + " Attempt: " + numberOfAttempts++);
                // Wait for handshake_ack from the server
                synchronized (this) {
                    this.wait(5000);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.clients.get(clientKey);
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
