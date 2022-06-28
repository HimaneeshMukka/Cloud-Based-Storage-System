package core;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class RUDP implements Closeable {
    private final RUDPPacketReceiverManager receiverManager = new RUDPPacketReceiverManager();
    private final RUDPPacketSenderManager senderManager = new RUDPPacketSenderManager();
    private InetAddress previousDataReceivedAddress;
    private int previousDataReceivedPort;
    private final DatagramSocket socket;


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

    public void send(RUDPDataPacket dataPacket, InetAddress destinationAddress, int destinationPortNumber) throws IOException {
        final byte[] data = this.convertObjectToByteArray(dataPacket);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destinationAddress, destinationPortNumber);
        this.socket.send(datagramPacket);
    }

    public void send(RUDPDataPacket dataPacket) throws IOException {
        assert this.previousDataReceivedAddress != null;
        this.send(dataPacket, this.previousDataReceivedAddress, this.previousDataReceivedPort);
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
