package stun.peer.travelers;

import stun.STUNException;

public class TravelPlan {
    private long activate;
    private long try_connect;

    /**
     * Travel Plan
     * @param activate when to send activate packet,
     *                 -1 meaning the operation is not required,
     *                 a pasted time meaning do it now
     * @param try_connect when to connect
     */
    public TravelPlan(long activate, long try_connect) {
        this.activate = activate;
        this.try_connect = try_connect;
    }

    public static TravelPlan parse(String str) throws STUNException {
        String[] frags = str.split(",");
        if (frags.length != 2)
            throw new STUNException("String given is invalid for travel plan");

        return new TravelPlan(Long.valueOf(frags[0]), Long.valueOf(frags[1]));
    }

    public String toString() {
        return activate + "," + try_connect;
    }

    public long getActivate() {
        return activate;
    }

    public void setActivate(long activate) {
        this.activate = activate;
    }

    public long getTry_connect() {
        return try_connect;
    }

    public void setTry_connect(long try_connect) {
        this.try_connect = try_connect;
    }
}
