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
    public boolean debug = false;

    private final ConcurrentMap<String, RUDPSocket> clients = new ConcurrentHashMap<>();
    private final StringBuilder isListening = new StringBuilder("false");
    private NewConnectionCallBack newConnectionCallBack = null;


    /**
     * Opens a UDP socket with random available port number. Use this for client connections
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
    public RUDP(int portNumber, NewConnectionCallBack newConnectionCallBack) throws SocketException {
        this.socket = new DatagramSocket(portNumber);
        this.newConnectionCallBack = newConnectionCallBack;
        this.listen();
    }

    public synchronized void listen() {
        // Receiver

        // Idempotent operation (if already listening, do nothing)
        if (Boolean.parseBoolean(this.isListening.toString())) {
            if (this.debug) System.out.println("Already listening");
            return;
        }
        this.isListening.setLength(0);
        this.isListening.append("true");
        if (this.debug) System.out.println("Listening");
        new Thread(() -> {
            try {
                while (true) {
                    if (this.debug) System.out.println("\n\nWaiting for data...");
                    byte[] buffer = new byte[100000000]; // 100Mb or 95MB
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                    this.socket.receive(datagramPacket);
                    RUDPDataPacket dataPacket = this.convertByteArrayToObject(datagramPacket.getData());

                    String clientKey = datagramPacket.getAddress().getHostAddress() + ":" + datagramPacket.getPort();

                    if (dataPacket == null) {
                        if (this.debug) System.out.println("Received null packet from: " + clientKey);
                        continue;
                    }
                    switch (dataPacket.type) {
                        case HANDSHAKE:
                            if (this.debug)
                                System.out.println("***********************\nReceived handshake from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            RUDPSocket newClient = new RUDPSocket(this.socket, datagramPacket.getAddress(), datagramPacket.getPort());
                            this.clients.put(clientKey, newClient);
                            RUDPDataPacket handshakeACK = new RUDPDataPacket(dataPacket.sequenceID, RUDPDataPacketType.HANDSHAKE_ACK);
                            newClient.sendAck(handshakeACK);

                            if (this.newConnectionCallBack != null) {
                                new Thread(() -> {
                                    try {
                                        this.newConnectionCallBack.onNewConnection(newClient);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }).start();
                            }
                            break;
                        case HANDSHAKE_ACK:
                            if (this.debug)
                                System.out.println("***********************\nReceived handshake_ack from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            // This means we have already created a client, and now we remove it from the waitingForACKS map
                            if (this.clients.containsKey(clientKey)) {
                                this.clients.get(clientKey).receive(dataPacket);
                            }
                            // This happens when we sent a HANDSHAKE packet to the server directly and now server has returned ACK.
                            else {
                                RUDPSocket newClient1 = new RUDPSocket(this.socket, datagramPacket.getAddress(), datagramPacket.getPort());
                                this.clients.put(clientKey, newClient1);
                                if (this.newConnectionCallBack != null) {
                                    new Thread(() -> {
                                        try {
                                            this.newConnectionCallBack.onNewConnection(newClient1);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).start();
                                }
                            }
                            break;
                        case ACK:
                            if (this.clients.containsKey(clientKey)) {
                                if (this.debug)
                                    System.out.println("***********************\nReceived ack from: " + clientKey + "\n" + dataPacket + "\n***********************");
                                this.clients.get(clientKey).receive(dataPacket);
                            }
                            else {
                                if (this.debug) System.out.println("Received ack from unknown client: " + clientKey);
                            }
                            break;
                        case NAK:
                            if (this.clients.containsKey(clientKey)) {
                                if (this.debug)
                                    System.out.println("***********************\nReceived nak from: " + clientKey + "\n" + dataPacket + "\n***********************");

                                this.clients.get(clientKey).receive(dataPacket);
                                this.clients.get(clientKey).retransmitPacket(dataPacket.sequenceID);

                            }
                            else {
                                if (this.debug) System.out.println("Received nak from unknown client: " + clientKey);
                            }
                            break;
                        case EOD:
                            if(this.debug) System.out.println("***********************\nReceived eod from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            if (this.clients.containsKey(clientKey)) {
                                this.clients.get(clientKey).receive(dataPacket);
                            }
                            else {
                                if (this.debug) System.out.println("Received eod from unknown client: " + clientKey);
                            }
                        case DATA:
                            if (this.debug)
                                System.out.println("***********************\nReceived data from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            if (this.clients.containsKey(clientKey)) {
                                this.clients.get(clientKey).receive(dataPacket);
                                break;
                            }
                        default:
                            if (this.debug)
                                System.out.println("***********************\nReceived unknown data/client from: " + clientKey + "\n" + dataPacket + "\n***********************");
                            break;

                    }

                }
            } catch (Exception e) {
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

    /**
     * Connect to the server. Use this for client side communication with the server. Blocking operation.
     * @param destinationAddress Address of the server.
     * @param destinationPortNumber Port number of the server.
     * @return Returns a socket object to send and receive data.
     */
    public RUDPSocket connect(InetAddress destinationAddress, int destinationPortNumber) {
        // Send a handshake packet to the server
        if (this.clients.containsKey(destinationAddress.getHostAddress() + ":" + destinationPortNumber)) {
            if (this.debug)
                System.out.println("Already connected to: " + destinationAddress.getHostAddress() + ":" + destinationPortNumber);
            return this.clients.get(destinationAddress.getHostAddress() + ":" + destinationPortNumber);
        }
        RUDPDataPacket handshake = new RUDPDataPacket(0, RUDPDataPacketType.HANDSHAKE);
        String clientKey = destinationAddress.getHostAddress() + ":" + destinationPortNumber;
        int numberOfAttempts = 1;
        while (!this.clients.containsKey(clientKey)) {
            try {
                this.send(handshake, destinationAddress, destinationPortNumber);
                if (this.debug)
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

    public RUDPSocket getClient(InetAddress destinationAddress, int destinationPortNumber) {
        return this.clients.get(destinationAddress.getHostAddress() + ":" + destinationPortNumber);
    }

    public ConcurrentMap<String, RUDPSocket> getClients() {
        return this.clients;
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
