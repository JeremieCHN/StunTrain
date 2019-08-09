package timesync;

import java.io.IOException;
import java.net.*;

public class TimeServer {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        InetAddress ip = InetAddress.getByName("192.168.12.213");
        int port = 12001;

        TimeServer server = new TimeServer(ip, port);
        server.start();
    }

    private DatagramSocket socket;

    TimeServer(InetAddress ip, int port) throws SocketException {
        socket = new DatagramSocket(new InetSocketAddress(ip, port));
    }

    void start() {
        System.out.println("Time Server Start:" + socket.getLocalSocketAddress());

        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[100], 100);
                socket.receive(packet);
                String timestamp = Long.toString(System.currentTimeMillis());
                DatagramPacket response = new DatagramPacket(timestamp.getBytes(), timestamp.getBytes().length);
                response.setSocketAddress(packet.getSocketAddress());
                socket.send(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Time Server broken");
    }
}
