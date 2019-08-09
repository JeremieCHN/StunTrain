import stun.peer.NatType;
import stun.peer.StunPeer;
import stun.peer.travelers.TravelPlan;
import stun.STUNException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class test {
    public static void main(String[] args) throws Exception {
        NetDetectorTest();
//        Travel();
    }

    private static void Travel() throws Exception {
        InetSocketAddress stunServer = new InetSocketAddress(InetAddress.getByName("stun.sipgate.net"), 10000);
        InetSocketAddress timeServer = new InetSocketAddress("TimeServer", 8080);

        StunPeer peer1 = new StunPeer(InetAddress.getByName("IP1"),stunServer,timeServer);
        StunPeer peer2 = new StunPeer(InetAddress.getByName("IP2"),stunServer,timeServer);

        while (true) {
            peer1.refreshPort();
            peer2.refreshPort();
            System.out.println("Peer1: " + peer1.getLocalInfo());
            System.out.println("Peer2: " + peer2.getLocalInfo());

            peer1.setRemoteInfo(peer2.getLocalInfo());
            peer2.setRemoteInfo(peer1.getLocalInfo());

            TravelPlan peerPlan = peer1.makePlan(3000);
            peer2.setPlan(peerPlan);

            Thread t1 = new Thread(() -> {
                try {
                    System.out.println("peer1:" + peer1.executePlan());
                } catch (STUNException | InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    System.out.println("peer2:" + peer2.executePlan());
                } catch (STUNException | InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
    }

    private static void NetDetectorTest() throws Exception {
//        InetSocketAddress stunServer = new InetSocketAddress(InetAddress.getByName("stun.sipgate.net"), 10000);
        InetSocketAddress stunServer = new InetSocketAddress(InetAddress.getByName("stun.sipgate.net"), 10000);

        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if (Class.forName("java.net.Inet4Address").isInstance(iaddress)) {
                    if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress())) {
                        System.out.println(iaddress.toString() + " " + NatType.getNatType(iaddress, stunServer).toString());
                    }
                }
            }
        }
    }
}
