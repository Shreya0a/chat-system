package com.lanmessenger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.lanmessenger.server.ChatServer;
import com.lanmessenger.server.WebSocketServer;
import com.lanmessenger.util.NetworkUtils;

/**
 * Entry point for the chat server.
 *
 *   java com.lanmessenger.Server [chat-port [web-port]]
 *
 * Two endpoints are started:
 *  - chat-port (default 5000): terminal clients via the Java client
 *  - web-port (default 8080):  the browser chat + the compiled React app
 */
public class Server {

    public static final int DEFAULT_WEB_PORT = 8080;

    public static void main(String[] args) {
        int port = args.length > 0 ? NetworkUtils.parsePort(args[0]) : NetworkUtils.DEFAULT_PORT;
        int webPort = args.length > 1 ? NetworkUtils.parsePort(args[1]) : DEFAULT_WEB_PORT;

        System.out.println("[LAN Messenger] Chat server starting...");
        System.out.println("[LAN Messenger] Host IP : " + NetworkUtils.getLocalIP());
        System.out.println("[LAN Messenger] TCP port : " + port);
        System.out.println("[LAN Messenger] Web port : " + webPort);

        ChatServer chat = new ChatServer(port);
        Path webRoot = Paths.get("web", "dist");

        try {
            WebSocketServer webServer = new WebSocketServer(webPort, chat, webRoot);
            Thread webThread = new Thread(() -> {
                try {
                    webServer.start();
                } catch (IOException e) {
                    System.err.println("[WEB] Cannot start on port " + webPort + ": " + e.getMessage());
                    System.err.println("[WEB] Build the React app first: cd web && npm install && npm run build");
                }
            }, "web-server");
            webThread.setDaemon(false);
            webThread.start();

            chat.start();
        } catch (IOException e) {
            System.err.println("[SERVER] Cannot start on port " + port + ": " + e.getMessage());
            System.err.println("[SERVER] Is another server already running?");
            System.exit(1);
        }
    }
}