package github.codeReaper2001.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class IPUtil {

    static volatile InetAddress hostIP = null;

    static public String getLocalHostIPStr() throws UnknownHostException {
        if (hostIP == null) {
            synchronized (IPUtil.class) {
                if (hostIP == null) {
                    hostIP = getLocalIpAddress();
                }
            }
        }
        return hostIP.getHostAddress();
    }

    private final static byte INVALID_MACS[][] = {
            {0x00, 0x05, 0x69},             // VMWare
            {0x00, 0x1C, 0x14},             // VMWare
            {0x00, 0x0C, 0x29},             // VMWare
            {0x00, 0x50, 0x56},             // VMWare
            {0x08, 0x00, 0x27},             // Virtualbox
            {0x0A, 0x00, 0x27},             // Virtualbox
            {0x00, 0x03, (byte) 0xFF},       // Virtual-PC
            {0x00, 0x15, 0x5D}              // Hyper-V
    };

    public static boolean isVMMac(byte[] mac) {
        if (null == mac) {
            return false;
        }

        for (byte[] invalid : INVALID_MACS) {
            if (invalid[0] == mac[0] && invalid[1] == mac[1] && invalid[2] == mac[2]) {
                return true;
            }
        }

        return false;
    }

    public static InetAddress getLocalIpAddress() throws UnknownHostException {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }

                if (isVMMac(ni.getHardwareAddress())) {
                    continue;
                }


                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isLinkLocalAddress()) {
                        continue;
                    }

                    return inetAddress;
                }
            }
        } catch (SocketException e) {
            throw new UnknownHostException("获取本机IP地址失败。");
        }
        throw new UnknownHostException("获取本机IP地址失败。");
    }
}
