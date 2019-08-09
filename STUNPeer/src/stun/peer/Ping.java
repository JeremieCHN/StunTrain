package stun.peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Ping {
    public static int ping(InetAddress src, InetAddress dst) {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface = null;

            NFACE:
            while (networkInterfaceEnumeration.hasMoreElements()) {
                networkInterface = networkInterfaceEnumeration.nextElement();
                Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                while (inetAddressEnumeration.hasMoreElements()) {
                    if (src.equals(inetAddressEnumeration.nextElement())) {
                        break NFACE;
                    }
                }
            }

            if (networkInterface != null) {
                int count = 0;
                long sum = 0;

                for (int i = 0; i < 5; i++) {
                    long start = System.currentTimeMillis();
                    if (dst.isReachable(networkInterface, 128, 3000)) {
                        count++;
                        sum += System.currentTimeMillis() - start;
                    }
                }
                if (count == 0)
                    return -1;
                else
                    return (int) (sum / count);
            }
        } catch (IOException ignored) {
        }
        return -1;
    }
/*
    // 才用了CMD ping命令的实现
    public static int ping(InetAddress src, InetAddress dst) throws STUNException {
        try {
            Process process = Runtime.getRuntime().exec("cmd");

            PrintWriter writer = new PrintWriter(process.getOutputStream());
            writer.println("chcp 65001");
            writer.println("ping " + dst.getHostAddress() + " /S " + src.getHostAddress() + " /n 10");
            writer.println("exit");
            writer.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                builder.append(tmp);
            }

            Pattern pattern = Pattern.compile("Average = (\\d+)ms");
            Matcher matcher = pattern.matcher(builder.toString());
            if (matcher.find()) {
                String out = matcher.group().substring(10);
                return Integer.parseInt(out.substring(0, out.length() - 2));
            }
        } catch (IOException e) {
            throw new STUNException("Ping test Failed", e);
        }
        throw new STUNException("Ping test Failed");
    }
*/
}
