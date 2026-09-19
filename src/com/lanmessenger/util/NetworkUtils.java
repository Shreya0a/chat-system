package com.lanmessenger.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Small helpers for finding the machine's LAN address and parsing arguments.
 */
public final class NetworkUtils {

    public static final int DEFAULT_PORT = 5000;

    private NetworkUtils() {
    }

    /**
     * Returns the first non-loopback IPv4 address belonging to this machine,
     * or "localhost" if none can be found.
     */
    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "localhost";
    }

    /** Parse a port from a command-line argument, falling back to the default. */
    public static int parsePort(String token) {
        try {
            int port = Integer.parseInt(token);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("port out of range");
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port '" + token + "', using " + DEFAULT_PORT + ".");
            return DEFAULT_PORT;
        }
    }

    /** Parses a host token, defaulting to "localhost". */
    public static String parseHost(String token) {
        return token == null || token.isEmpty() ? "localhost" : token;
    }
}