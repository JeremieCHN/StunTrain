package stun.peer;

import stun.STUNException;

import java.net.InetSocketAddress;

public class StunInfo {
    private InetSocketAddress wanAddr;
    private long wanDelay;
    private NatType natType;

    StunInfo() {
    }

    public StunInfo(InetSocketAddress wanAddr, long wanDelay, NatType natType) {
        this.wanAddr = wanAddr;
        this.wanDelay = wanDelay;
        this.natType = natType;
    }

    public static StunInfo parse(String str) throws STUNException {
        String[] flags = str.split("\\|");
        if (flags.length != 3)
            throw new STUNException("String given cannot be parsed");

        StunInfo info = new StunInfo();
        if (flags[0].equalsIgnoreCase("null")) {
            info.wanAddr = null;
        } else {

            String[] addrFlags = flags[0].split(":");
            if (addrFlags[0].startsWith("/")) {
                info.wanAddr = new InetSocketAddress(addrFlags[0].substring(1), Integer.parseInt(addrFlags[1]));
            } else {
                info.wanAddr = new InetSocketAddress(addrFlags[0], Integer.parseInt(addrFlags[1]));
            }
        }
        info.wanDelay = Long.valueOf(flags[1]);
        info.natType = NatType.parseStr(flags[2]);
        return info;
    }

    public String toString() {
        return wanAddr.toString() + "|" + wanDelay + "|" + natType.toString();
    }

    public InetSocketAddress getWanAddr() {
        return wanAddr;
    }

    public void setWanAddr(InetSocketAddress wanAddr) {
        this.wanAddr = wanAddr;
    }

    public long getWanDelay() {
        return wanDelay;
    }

    public void setWanDelay(long wanDelay) {
        this.wanDelay = wanDelay;
    }

    public NatType getNatType() {
        return natType;
    }

    public void setNatType(NatType natType) {
        this.natType = natType;
    }
}
