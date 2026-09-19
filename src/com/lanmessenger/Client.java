package com.lanmessenger;

import java.io.IOException;

import com.lanmessenger.client.ChatClient;
import com.lanmessenger.util.NetworkUtils;

/**
 * Entry point for the chat client.
 *
 *   java com.lanmessenger.Client [host [port]] [username]
 */
public class Client {

    public static void main(String[] args) {
        String host = NetworkUtils.parseHost(args.length > 0 ? args[0] : null);
        int port = args.length > 1 ? NetworkUtils.parsePort(args[1]) : NetworkUtils.DEFAULT_PORT;
        String username = args.length > 2 ? args[2] : promptForUsername();

        try {
            new ChatClient(host, port, username).start();
        } catch (IOException e) {
            System.err.println("[CLIENT] Could not connect to " + host + ":" + port + ": " + e.getMessage());
            System.err.println("[CLIENT] Is the server running?");
            System.exit(1);
        }
    }

    private static String promptForUsername() {
        try {
            java.io.BufferedReader console = new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
            System.out.print("Username: ");
            System.out.flush();
            String name = console.readLine();
            return name == null || name.trim().isEmpty() ? "Anonymous" : name.trim();
        } catch (IOException e) {
            return "Anonymous";
        }
    }
}