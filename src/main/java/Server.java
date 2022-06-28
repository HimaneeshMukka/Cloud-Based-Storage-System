import core.RUDP;
import core.RUDPDataPacket;

import java.io.*;

public class Server {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        RUDP server = new RUDP(5000);
        RUDPDataPacket data = server.receive();
        System.out.println("Received data: " + data);
        data = new RUDPDataPacket(1, "Test from server");
        server.send(data);
        server.close();
    }
}
