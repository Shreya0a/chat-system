package com.lanmessenger.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.lanmessenger.model.Message;
import com.lanmessenger.model.Message.Type;

/**
 * Interactive console client.
 *
 * One thread pumps incoming messages onto the screen while the main thread
 * reads what the user types. Anything starting with '/' is treated as a
 * command; everything else is a public chat message.
 */
public class ChatClient {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final String host;
    private final int port;
    private final BufferedReader console;
    private final String username;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;

    public ChatClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    public void start() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("Connected to " + host + ":" + port + ". Type /help for commands.");
        out.println(Message.now(Type.JOIN, username, "", username).serialize());

        Thread receiver = new Thread(this::receiveLoop, "message-receiver");
        receiver.setDaemon(true);
        receiver.start();

        sendLoop();

        shutdown();
    }

    private void sendLoop() {
        try {
            String line;
            while (running && (line = console.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("/")) {
                    handleCommand(line.trim());
                } else {
                    out.println(Message.now(Type.PUBLIC, username, "", line).serialize());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Console error: " + e.getMessage());
            }
        }
    }

    private void handleCommand(String line) throws IOException {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        switch (command) {
            case "/help":
                printHelp();
                break;
            case "/users":
                out.println(Message.now(Type.USERLIST, username, "", "").serialize());
                break;
            case "/msg":
            case "/pm":
                if (parts.length < 3) {
                    System.out.println("Usage: /msg <username> <message>");
                    break;
                }
                String recipient = parts[1];
                String text = line.substring(line.indexOf(parts[1]) + parts[1].length()).trim();
                out.println(Message.now(Type.PRIVATE, username, recipient, text).serialize());
                break;
            case "/quit":
            case "/exit":
                running = false;
                out.println(Message.now(Type.LEAVE, username, "", "").serialize());
                break;
            default:
                System.out.println("Unknown command: " + command + " (try /help)");
        }
    }

    private void printHelp() {
        System.out.println("  <text>           send a public message");
        System.out.println("  /msg <name> <t>  send a private message to <name>");
        System.out.println("  /users           list online users");
        System.out.println("  /help            show this help");
        System.out.println("  /quit, /exit     leave the chat");
    }

    private void receiveLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                Message message = Message.deserialize(line);
                render(message);
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[SERVER] Lost connection to the server.");
            }
        } finally {
            running = false;
        }
    }

    private void render(Message message) {
        switch (message.getType()) {
            case PUBLIC:
                renderPublic(message);
                break;
            case PRIVATE:
                renderPrivate(message);
                break;
            case SERVER:
                System.out.println("[SERVER] " + message.getContent());
                break;
            case HISTORY:
                renderHistory(message);
                break;
            case USERLIST:
                System.out.println("Users online: " + message.getContent());
                break;
            default:
                break;
        }
    }

    private void renderPublic(Message message) {
        String who = message.getSender().equalsIgnoreCase(username)
                ? "You"
                : message.getSender();
        System.out.println("[" + formatTime(message.getTimestamp()) + "] " + who + ": " + message.getContent());
    }

    private void renderPrivate(Message message) {
        if (message.getSender().equalsIgnoreCase(username)) {
            System.out.println("[" + formatTime(message.getTimestamp()) + "] You -> "
                    + message.getRecipient() + " (private): " + message.getContent());
        } else {
            System.out.println("[" + formatTime(message.getTimestamp()) + "] "
                    + message.getSender() + " (private): " + message.getContent());
        }
    }

    private void renderHistory(Message message) {
        if (message.getContent() == null || message.getContent().isEmpty()) {
            System.out.println("[SERVER] No previous messages in this session.");
            return;
        }
        System.out.println("[SERVER] --- chat history for this session ---");
        for (String line : message.getContent().split("\n")) {
            render(Message.deserialize(line));
        }
        System.out.println("[SERVER] --- end of history ---");
    }

    private static String formatTime(long millis) {
        if (millis <= 0) {
            return "--:--";
        }
        return LocalTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(TIME);
    }

    private void shutdown() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        try {
            System.in.close();
        } catch (IOException ignored) {
        }
    }
}