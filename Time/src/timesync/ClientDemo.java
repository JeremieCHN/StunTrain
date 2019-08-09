package timesync;

import java.io.IOException;

public class ClientDemo {
    public static void main(String[] args) throws IOException {
        System.out.println("timesync.TimeClient Test");
        System.out.println("TimeStamp: " + TimeClient.getServerTime("192.168.12.191", 12001));

    }
}
