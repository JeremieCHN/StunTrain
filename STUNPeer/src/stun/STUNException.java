package stun;

public class STUNException extends Exception {
    public STUNException(String msg) {
        super(msg);
    }
    public STUNException(String msg, Throwable e) {
        super(msg, e);
    }
}
