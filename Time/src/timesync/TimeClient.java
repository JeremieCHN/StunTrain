package timesync;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class TimeClient {
    public static long getServerTime(String server_ip, int server_port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket("1".getBytes(), "1".getBytes().length);
        packet.setSocketAddress(new InetSocketAddress(server_ip, server_port));
        socket.send(packet);
        socket.setSoTimeout(3000);
        DatagramPacket res = new DatagramPacket(new byte[100], 100);
        socket.receive(res);
        return Long.valueOf(new String(Arrays.copyOf(res.getData(), res.getLength())));
    }
}
