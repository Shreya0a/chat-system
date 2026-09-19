package com.lanmessenger.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.lanmessenger.model.Message;

/**
 * One thread per connected terminal client. Reads tab-delimited wire-format
 * messages off the socket and delegates routing to the shared
 * {@link ChatServer}.
 */
public class ClientHandler implements ChatClientConnection, Runnable {

    private final Socket socket;
    private final ChatServer server;

    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public synchronized String getUsername() {
        return username;
    }

    @Override
    public synchronized void setUsername(String username) {
        this.username = username;
    }

    @Override
    public synchronized void sendMessage(Message message) {
        if (out != null) {
            out.println(message.serialize());
        }
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true);
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    server.route(this, Message.deserialize(line));
                } catch (Exception e) {
                    System.out.println("[SERVER] Bad message from " + socket.getRemoteSocketAddress() + ": " + e);
                }
            }
        } catch (IOException e) {
            // expected when the peer drops the connection
        } finally {
            server.clientLeft(this, true);
        }
    }

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}