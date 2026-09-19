package com.lanmessenger.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.lanmessenger.model.Message;
import com.lanmessenger.model.Message.Type;

/**
 * Accepts socket connections and coordinates everything happening on the chat.
 *
 * The server itself is dumb about message content: it routes messages from
 * client to client, fills the per-session history buffer, and emits the
 * join/leave notices.
 */
public class ChatServer {

    /** Hard cap on the number of history entries remembered per session. */
    private static final int MAX_HISTORY = 200;

    private final int port;
    private final List<ChatClientConnection> clients = new CopyOnWriteArrayList<>();
    private final List<Message> history = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

    public ChatServer(int port) {
        this.port = port;
    }

    /** Accept connections forever. Never returns. */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] Listening on port " + port);
            System.out.println("[SERVER] Clients must connect to the IP shown on startup.");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                addConnection(handler);
                System.out.println("[SERVER] TCP connection from " + socket.getInetAddress().getHostAddress());
                new Thread(handler, "handler-" + socket.getPort()).start();
            }
        }
    }

    /** Register a connection that has not yet chosen a username. */
    public void addConnection(ChatClientConnection client) {
        clients.add(client);
        System.out.println("[SERVER] New client connected (" + clients.size() + " online).");
    }

    /** Register this client connection under the requested name. Returns false if the name is taken. */
    public synchronized boolean addClient(ChatClientConnection client, String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        for (ChatClientConnection other : clients) {
            if (other != client
                    && other.getUsername() != null
                    && other.getUsername().equalsIgnoreCase(username)) {
                return false;
            }
        }
        client.setUsername(username.trim());
        return true;
    }

    /** Called by a connection when its client disconnects, for any reason. */
    public void clientLeft(ChatClientConnection client, boolean announced) {
        String username = client.getUsername();
        if (clients.remove(client)) {
            if (username != null && announced) {
                broadcast(Message.now(Type.SERVER, "SERVER", "", username + " left the chat."));
                broadcastUserList();
            }
            System.out.println("[SERVER] " + (username == null ? "anonymous" : username) + " disconnected.");
        }
        client.close();
    }

    /** Send to every connected client. */
    public void broadcast(Message message) {
        broadcast(message, null);
    }

    /** Send this message to every connected client except the given one. */
    public void broadcast(Message message, ChatClientConnection exclude) {
        for (ChatClientConnection client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    /** Send a message to a single named recipient. Returns true if delivered. */
    public boolean sendTo(String recipient, Message message) {
        for (ChatClientConnection client : clients) {
            if (recipient != null && recipient.equalsIgnoreCase(client.getUsername())) {
                client.sendMessage(message);
                return true;
            }
        }
        return false;
    }

    /** Remember a message so late joiners can see what happened earlier. */
    public void appendHistory(Message message) {
        history.add(message);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    /** Snapshot of everything that happened so far, oldest first. */
    public List<Message> getHistory() {
        return new java.util.ArrayList<>(history);
    }

    public String[] getUsernames() {
        return clients.stream()
                .map(ChatClientConnection::getUsername)
                .filter(u -> u != null)
                .toArray(String[]::new);
    }

    public String formatUserList() {
        String[] names = getUsernames();
        if (names.length == 0) {
            return "No other users online.";
        }
        return String.join(", ", names);
    }

    /** Push the current online users to every connected client. */
    public void broadcastUserList() {
        broadcast(Message.now(Type.USERLIST, "SERVER", "", String.join(", ", getUsernames())));
    }

    /** Time used inside server-produced messages. */
    public String time(long millis) {
        return LocalTime.ofInstant(java.time.Instant.ofEpochMilli(millis),
                java.time.ZoneId.systemDefault()).format(timeFormat);
    }

    /**
     * Route one incoming message from any kind of client (TCP or WebSocket).
     * Shared by all connection types so behaviour stays identical everywhere.
     */
    public void route(ChatClientConnection client, Message message) {
        switch (message.getType()) {
            case JOIN:
                handleJoin(client, message);
                break;
            case PUBLIC:
                handlePublic(client, message);
                break;
            case PRIVATE:
                handlePrivate(client, message);
                break;
            case LEAVE:
                clientLeft(client, false);
                break;
            case USERLIST:
                client.sendMessage(Message.now(Type.USERLIST, "SERVER",
                        client.getUsername(), formatUserList()));
                break;
            default:
                client.sendMessage(Message.now(Type.SERVER, "SERVER", "",
                        "Unsupported message type: " + message.getType()));
        }
    }

    private void handleJoin(ChatClientConnection client, Message message) {
        String existing = client.getUsername();
        if (existing != null) {
            client.sendMessage(Message.now(Type.SERVER, "SERVER", "", "You are already registered."));
            return;
        }
        String requested = message.getContent();
        if (!addClient(client, requested)) {
            client.sendMessage(Message.now(Type.SERVER, "SERVER", "",
                    "Sorry, that username is taken or empty. Try another name."));
            return;
        }
        String username = client.getUsername();
        client.sendMessage(Message.now(Type.SERVER, "SERVER", "", "Welcome, " + username + "!"));
        client.sendMessage(Message.now(Type.SERVER, "SERVER", "",
                "Type /help to see available commands. Users online: " + formatUserList()));

        // Chat history during the session: replay what happened before we joined.
        StringBuilder historyText = new StringBuilder();
        String historySeparator = System.lineSeparator();
        for (Message past : getHistory()) {
            historyText.append(past.serialize()).append(historySeparator);
        }
        client.sendMessage(new Message(Type.HISTORY, "SERVER", username,
                System.currentTimeMillis(), historyText.toString()));

        Message join = Message.now(Type.SERVER, "SERVER", "", username + " joined the chat.");
        broadcast(join, client);
        broadcastUserList();
        appendHistory(join);
    }

    private void handlePublic(ChatClientConnection client, Message message) {
        String text = message.getContent() == null ? "" : message.getContent().trim();
        if (text.isEmpty()) {
            return;
        }
        Message publicMsg = Message.now(Type.PUBLIC, client.getUsername(), "", text);
        broadcast(publicMsg, client);
        appendHistory(publicMsg);
        // Echo back to the sender so they see their own message as "you".
        client.sendMessage(publicMsg);
    }

    private void handlePrivate(ChatClientConnection client, Message message) {
        String recipient = message.getRecipient();
        if (recipient == null || recipient.trim().isEmpty()) {
            client.sendMessage(Message.now(Type.SERVER, "SERVER", "", "Private message needs a recipient."));
            return;
        }
        String text = message.getContent() == null ? "" : message.getContent().trim();
        if (text.isEmpty()) {
            return;
        }
        Message pm = Message.now(Type.PRIVATE, client.getUsername(), recipient.trim(), text);
        if (!sendTo(recipient.trim(), pm)) {
            client.sendMessage(Message.now(Type.SERVER, "SERVER", "",
                    "User '" + recipient + "' is not online."));
            return;
        }
        // Confirmation copy for the sender.
        client.sendMessage(pm);
    }
}
