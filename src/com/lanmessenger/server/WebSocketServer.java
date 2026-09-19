package com.lanmessenger.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Serves two things from one plain TCP port:
 *
 *  - {@code /ws} : a WebSocket endpoint the browser chat talks to
 *  - everything else: static files from the built React app ({@code web/dist})
 *
 * The handshake and WebSocket frame handling are implemented from scratch so
 * the project stays dependency-free.
 */
public class WebSocketServer {

    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final String UPGRADE_PATH = "/ws";
    private static final int MAX_REQUEST_HEAD = 16 * 1024;

    private final int port;
    private final ChatServer chatServer;
    private final Path staticRoot;
    private final Map<String, String> contentTypes = new HashMap<>();

    public WebSocketServer(int port, ChatServer chatServer, Path staticRoot) {
        this.port = port;
        this.chatServer = chatServer;
        this.staticRoot = staticRoot;
        contentTypes.put("html", "text/html; charset=utf-8");
        contentTypes.put("js", "application/javascript");
        contentTypes.put("css", "text/css; charset=utf-8");
        contentTypes.put("json", "application/json");
        contentTypes.put("svg", "image/svg+xml");
        contentTypes.put("png", "image/png");
        contentTypes.put("ico", "image/x-icon");
        contentTypes.put("map", "application/json");
    }

    /** Accept connections forever. Never returns. */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[WEB] Browser endpoint listening on http://" + serverSocket.getInetAddress().getHostAddress()
                    + ":" + port + "/");
            while (true) {
                Socket socket = serverSocket.accept();
                handle(socket);
            }
        }
    }

    private void handle(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            String requestHead = readRequestHead(in);
            if (requestHead.isEmpty()) {
                socket.close();
                return;
            }
            String normalizedHead = requestHead.toLowerCase(Locale.ROOT);
            if (normalizedHead.startsWith("get " + UPGRADE_PATH + " ")
                    && normalizedHead.contains("upgrade: websocket")) {
                // WebSocket connections outlive this method: the socket stays open
                // and is owned by the WebSocketConnection thread.
                upgradeToWebSocket(socket, in, requestHead);
            } else {
                serveStatic(socket, requestHead);
                socket.close();
            }
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** Reads bytes until the blank line that ends the HTTP request head. */
    private static String readRequestHead(InputStream in) throws IOException {
        StringBuilder head = new StringBuilder();
        int count = 0;
        int last4 = 0;
        while (count < MAX_REQUEST_HEAD && last4 < 4) {
            int b = in.read();
            if (b < 0) {
                break;
            }
            head.append((char) b);
            count++;
            switch (last4) {
                case 0: last4 = (b == '\r') ? 1 : 0; break;
                case 1: last4 = (b == '\n') ? 2 : ((b == '\r') ? 1 : 0); break;
                case 2: last4 = (b == '\r') ? 3 : 0; break;
                case 3: last4 = (b == '\n') ? 4 : 0; break;
                default: last4 = 4;
            }
        }
        return head.toString();
    }

    private void upgradeToWebSocket(Socket socket, InputStream in, String requestHead) throws IOException {
        String key = extractHeader(requestHead, "Sec-WebSocket-Key");
        if (key == null) {
            return;
        }
        String accept = computeAccept(key);
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();

        WebSocketConnection connection = new WebSocketConnection(socket, chatServer);
        chatServer.addConnection(connection);
        System.out.println("[WEB] WebSocket connection from " + socket.getInetAddress().getHostAddress());
        new Thread(connection, "ws-" + socket.getPort()).start();
    }

    private static String extractHeader(String head, String name) {
        String marker = name + ":";
        int idx = head.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        int start = idx + marker.length();
        while (start < head.length() && (head.charAt(start) == ' ' || head.charAt(start) == '\t')) {
            start++;
        }
        int end = head.indexOf("\r\n", start);
        if (end < 0) {
            end = head.length();
        }
        return head.substring(start, end).trim();
    }

    private static String computeAccept(String key) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest((key + WEBSOCKET_GUID).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    /** Serves compiled React assets from web/dist with a clean 404 fallback. */
    private void serveStatic(Socket socket, String requestHead) throws IOException {
        String requestLine = requestHead.split("\r\n", 2)[0];
        String[] parts = requestLine.split(" ");
        if (parts.length < 2 || !parts[0].equalsIgnoreCase("GET")) {
            writePlain(socket, 405, "Method Not Allowed", "Only GET is supported.");
            return;
        }
        String path = parts[1];
        if ("/".equals(path)) {
            path = "/index.html";
        }
        Path file = resolvePath(path);
        if (file == null || !Files.isRegularFile(file)) {
            writePlain(socket, 404, "Not Found", "File not found.");
            return;
        }
        String extension = extensionOf(path);
        String type = contentTypes.getOrDefault(extension, "application/octet-stream");
        byte[] body = Files.readAllBytes(file);
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK\r\n");
        response.append("Content-Type: ").append(type).append("\r\n");
        response.append("Content-Length: ").append(body.length).append("\r\n");
        response.append("Cache-Control: no-cache\r\n");
        response.append("\r\n");
        try (OutputStream out = socket.getOutputStream()) {
            out.write(response.toString().getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        }
    }

    /** Maps a request path onto web/dist, refusing any path traversal. */
    private Path resolvePath(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("../") || normalized.contains("..")) {
            return null;
        }
        return staticRoot.resolve(normalized).normalize();
    }

    private static String extensionOf(String path) {
        if (path == null) {
            return "";
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.indexOf('/') < 0 ? ext : "";
    }

    private void writePlain(Socket socket, int status, String reason, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: text/plain; charset=utf-8\r\n"
                + "Content-Length: " + payload.length + "\r\n\r\n";
        try (OutputStream out = socket.getOutputStream()) {
            out.write(head.getBytes(StandardCharsets.US_ASCII));
            out.write(payload);
            out.flush();
        }
    }
}
