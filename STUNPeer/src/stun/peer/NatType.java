package stun.peer;

import stun.STUNException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class NatType {
    private boolean open_access;
    private boolean cone;
    private boolean symmetric;
    private boolean refuse;

    private NatType() {
    }

    public static NatType getNatType(InetAddress localAddr, InetSocketAddress serverAddr)
            throws IOException, STUNException {
        NatDetector client = new NatDetector(localAddr, serverAddr);

        NatType nat_type = new NatType();
        nat_type.open_access = client.isOpenAccess();
        nat_type.cone = client.isCone();
        nat_type.symmetric = client.isSymmetric(nat_type.open_access);
        nat_type.refuse = client.isRefuse();

        return nat_type;
    }

    public static NatType parseStr(String nat) throws STUNException {
        if (nat.length() != 4)
            throw new STUNException("String given cannot parse to NAT_TYPE");

        NatType type = new NatType();
        type.open_access = nat.charAt(0) == 'T';
        type.cone = nat.charAt(1) == 'T';
        type.symmetric = nat.charAt(2) == 'T';
        type.refuse = nat.charAt(3) == 'T';

        return type;
    }

    public String toString() {
        return (open_access ? "T" : "F") +
                (cone ? "T" : "F") +
                (symmetric ? "T" : "F") +
                (refuse ? "T" : "F");
    }

    public boolean isOpenAccess() {
        return open_access;
    }

    public boolean isCone() {
        return cone;
    }

    public boolean isSymmetric() {
        return symmetric;
    }

    public boolean isRefuse() {
        return refuse;
    }

}