package stun.peer;


import stun.peer.travelers.TravelPlan;
import stun.peer.travelers.Traveler;
import stun.STUNException;
import timesync.TimeClient;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class StunPeer {
    // server information
    private InetSocketAddress stunServer;
    private InetSocketAddress timeServer;

    // local information
    private InetAddress localAddr;
    private StunInfo localInfo = null;

    // remote information
    private StunInfo remoteInfo = null;

    // thread hold wan addr
    private Thread wanHolder = null;

    // client connect with stunServer
    private NatDetector client = null;

    // udp socket
    private DatagramSocket socket = null;

    // stun plan
    private long timeDiff = 0;
    private TravelPlan localPlan;

    public StunPeer(InetAddress localAddr, InetSocketAddress stunServer, InetSocketAddress timeServer) {
        this.localAddr = localAddr;
        this.stunServer = stunServer;
        this.timeServer = timeServer;
    }

    public void refreshPort() throws STUNException {
        try {
            initLocalInfo();
            initWanHolder();
            timeSync();
        } catch (Exception e) {
            clear();
            throw new STUNException("Bind Port Failed", e);
        }

        if (localInfo.getNatType().isSymmetric()) {
            clear();
            throw new STUNException("Symmetric is unsupported now");
        }
    }

    public StunInfo getLocalInfo() {
        return localInfo;
    }

    public void setRemoteInfo(StunInfo remote) throws STUNException {
        this.remoteInfo = remote;
        if (remote.getNatType().isSymmetric())
            throw new STUNException("Symmetric is unsupported now");
    }

    public TravelPlan makePlan(long notifyDelay) throws STUNException {
        if (remoteInfo == null)
            throw new STUNException("Missing Remote Info");

        if (remoteInfo.getNatType().isSymmetric())
            throw new STUNException("Nat Type is not supported");

        // refuse = refuse
        if (remoteInfo.getNatType().isRefuse() && localInfo.getNatType().isRefuse()) {
            localPlan = new TravelPlan(
                    System.currentTimeMillis() + timeDiff + notifyDelay + remoteInfo.getWanDelay(),
                    -1);

            return new TravelPlan(
                    System.currentTimeMillis() + timeDiff + notifyDelay + localInfo.getWanDelay(),
                    -1);
        }

        // refuse = cone
        if (remoteInfo.getNatType().isRefuse() && (localInfo.getNatType().isCone() && !localInfo.getNatType().isRefuse())) {
            localPlan = new TravelPlan(
                    System.currentTimeMillis() + timeDiff + notifyDelay + remoteInfo.getWanDelay(),
                    System.currentTimeMillis() + timeDiff + notifyDelay + remoteInfo.getWanDelay() + 500);

            return new TravelPlan(
                    0,
                    System.currentTimeMillis() + timeDiff + notifyDelay + localInfo.getWanDelay() + 500);
        }

        // cone = refuse
        if ((remoteInfo.getNatType().isCone() && !remoteInfo.getNatType().isRefuse()) &&
                localInfo.getNatType().isRefuse()) {
            localPlan = new TravelPlan(
                    0,
                    System.currentTimeMillis() + timeDiff + notifyDelay + remoteInfo.getWanDelay() + 500);

            return new TravelPlan(
                    System.currentTimeMillis() + timeDiff + notifyDelay + localInfo.getWanDelay(),
                    System.currentTimeMillis() + timeDiff + notifyDelay + localInfo.getWanDelay() + 500);

        }

        // cone = cone
        if ((remoteInfo.getNatType().isCone() && !remoteInfo.getNatType().isRefuse()) &&
                (localInfo.getNatType().isCone() && !localInfo.getNatType().isRefuse())) {
            localPlan = new TravelPlan(
                    0,
                    System.currentTimeMillis() + timeDiff + notifyDelay + remoteInfo.getWanDelay() + 500);

            return new TravelPlan(
                    0,
                    System.currentTimeMillis() + timeDiff + notifyDelay + localInfo.getWanDelay() + 500);
        }

        // open = open
        if (remoteInfo.getNatType().isOpenAccess() && localInfo.getNatType().isOpenAccess()) {
            localPlan = new TravelPlan(-1, System.currentTimeMillis() + timeDiff + notifyDelay);
            return new TravelPlan(-1, System.currentTimeMillis() + timeDiff + notifyDelay);
        }

        // open = other
        if (remoteInfo.getNatType().isOpenAccess()) {
            localPlan = new TravelPlan(0, System.currentTimeMillis() + timeDiff + notifyDelay);
            return new TravelPlan(-1, System.currentTimeMillis() + timeDiff + notifyDelay);
        }

        // other = open
        if (localInfo.getNatType().isOpenAccess()) {
            localPlan = new TravelPlan(-1, System.currentTimeMillis() + timeDiff + notifyDelay);
            return new TravelPlan(0, System.currentTimeMillis() + timeDiff + notifyDelay);
        }

        throw new STUNException("Nat Type is not supported");
    }

    public void setPlan(TravelPlan plan) {
        this.localPlan = plan;
    }

    public boolean executePlan() throws STUNException, InterruptedException, IOException {

        if (remoteInfo == null || localPlan == null) {
            throw new STUNException("Necessary Info requested");
        }

        if (localPlan.getActivate() != 0 && localPlan.getActivate() != -1)
            localPlan.setActivate(localPlan.getActivate() - timeDiff);

        localPlan.setTry_connect(localPlan.getTry_connect() - timeDiff);

        Traveler traveler = new Traveler(socket, remoteInfo.getWanAddr(), localPlan);
        traveler.setOverTime(3000);
        return traveler.travel();
    }

    public void clear() {
        if (wanHolder != null)
            wanHolder.interrupt();
    }

    public DatagramSocket getSocket() {
        if (wanHolder != null)
            wanHolder.interrupt();
        return socket;
    }
    /*
     * initialize
     */
    private void initLocalInfo() throws IOException, STUNException {
        if (socket != null) socket.close();
        socket = new DatagramSocket(0, localAddr);
        client = new NatDetector(localAddr, stunServer);

        localInfo = new StunInfo();
        localInfo.setWanAddr(client.getWanAddress(socket));
        localInfo.setNatType(NatType.getNatType(localAddr, stunServer));
        if (localInfo.getNatType().isOpenAccess()) {
            localInfo.setWanDelay(0);
        } else {
//            localInfo.setWanDelay(Ping.ping(localAddr, localInfo.getWanAddr().getAddress()));
            localInfo.setWanDelay(Ping.ping(localAddr, stunServer.getAddress()));
        }
    }

    private void initWanHolder() {
        if (wanHolder != null) wanHolder.interrupt();
        wanHolder = new Thread(() -> {
            try {
                while (true) {
                    client.getWanAddress(socket);
                    Thread.sleep(3000);
                }
            } catch (STUNException | IOException | InterruptedException ignored) {
            }
        }, "wanHolder");
        wanHolder.start();
    }

    private void timeSync() throws IOException {
        for (int i = 0; i < 10; i++) {
            long t1 = System.currentTimeMillis();
            long t2 = TimeClient.getServerTime(timeServer.getHostString(), timeServer.getPort());
            long t3 = System.currentTimeMillis();
            timeDiff += ((t1 + t3) / 2 - t2);
        }
        timeDiff /= 10;
    }
}

