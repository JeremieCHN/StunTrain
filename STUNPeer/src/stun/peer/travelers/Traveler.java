package stun.peer.travelers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Traveler {
    private DatagramSocket socket;
    private InetSocketAddress remote;

    private long overTime = 3000;
    private TravelPlan plan;

    public Traveler(DatagramSocket socket, InetSocketAddress remote, TravelPlan plan) {
        this.socket = socket;
        this.remote = remote;
        this.plan = plan;
    }

    public boolean travel() throws InterruptedException, IOException {

        if (plan.getActivate() >= 0) {
            if (System.currentTimeMillis() < plan.getActivate())
                Thread.sleep(plan.getActivate() - System.currentTimeMillis());

            DatagramPacket activatePacket = new DatagramPacket("Activate".getBytes(), "Activate".getBytes().length);
            activatePacket.setSocketAddress(remote);
            for (int i = 0; i < 5; i++)
                socket.send(activatePacket);
        }

        if (System.currentTimeMillis() < plan.getTry_connect())
            Thread.sleep(plan.getTry_connect() - System.currentTimeMillis());

        socket.setSoTimeout(500);
        DatagramPacket req = new DatagramPacket("req".getBytes(), "req".getBytes().length);
        req.setSocketAddress(remote);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + overTime) {
            socket.send(req);
            try {
                DatagramPacket rcv = new DatagramPacket(new byte[3], 3);
                socket.receive(rcv);
                if (rcv.getSocketAddress().equals(remote)) {
                    rcv = new DatagramPacket("rcv".getBytes(), "rcv".getBytes().length);
                    rcv.setSocketAddress(remote);
                    for (int i = 0; i < 5; i++) socket.send(rcv);
                    return true;
                }
            } catch (IOException e) {
                if (!e.getMessage().startsWith("Receive timeout"))
                    break;
            }
        }

        return false;
    }

    public void setOverTime(long t) {
        this.overTime = t;
    }
}
