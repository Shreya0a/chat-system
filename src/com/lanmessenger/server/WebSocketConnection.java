package com.lanmessenger.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.lanmessenger.model.JsonUtil;
import com.lanmessenger.model.Message;

/**
 * One thread per connected browser client speaking WebSocket (RFC 6455).
 *
 * Frames come off the socket as JSON text messages, are decoded into a
 * {@link Message} and handed to the shared {@link ChatServer} for routing,
 * exactly like the terminal clients. Outgoing messages are JSON text frames.
 */
public class WebSocketConnection implements ChatClientConnection, Runnable {

    private final Socket socket;
    private final ChatServer server;

    private final Object writeLock = new Object();
    private volatile boolean running = true;
    private String username;

    public WebSocketConnection(Socket socket, ChatServer server) {
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
    public void sendMessage(Message message) {
        writeText(JsonUtil.toJson(message));
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            while (running) {
                Frame frame = Frame.read(in);
                switch (frame.opcode) {
                    case 0x1: // text
                        String json = new String(frame.payload, StandardCharsets.UTF_8);
                        try {
                            server.route(this, JsonUtil.fromJson(json));
                        } catch (Exception e) {
                            System.out.println("[SERVER] Bad WebSocket message: " + e);
                        }
                        break;
                    case 0x8: // close
                        running = false;
                        break;
                    case 0x9: // ping -> pong
                        writeFrame(0xA, frame.payload);
                        break;
                    case 0xA: // pong
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            // expected when the browser tab closes
        } finally {
            server.clientLeft(this, true);
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void writeText(String text) {
        writeFrame(0x1, text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeFrame(int opcode, byte[] payload) {
        synchronized (writeLock) {
            try {
                OutputStream out = socket.getOutputStream();
                out.write(0x80 | opcode);
                writeLength(out, payload.length);
                out.write(payload);
                out.flush();
            } catch (IOException ignored) {
                running = false;
            }
        }
    }

    private static void writeLength(OutputStream out, int length) throws IOException {
        if (length < 126) {
            out.write(length);
        } else if (length < 65536) {
            out.write(126);
            out.write(length >> 8);
            out.write(length);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) {
                out.write((length >> (8 * i)) & 0xFF);
            }
        }
    }

    /** A single decoded WebSocket data frame. */
    private static final class Frame {
        final int opcode;
        final byte[] payload;

        Frame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }

        /** Reads one frame from the stream, applying the client mask. */
        static Frame read(InputStream in) throws IOException {
            int b0 = in.read();
            int b1 = in.read();
            if (b0 < 0 || b1 < 0) {
                throw new IOException("connection closed");
            }
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long length = b1 & 0x7F;
            if (length == 126) {
                length = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) {
                    length = (length << 8) | (in.read() & 0xFF);
                }
            }
            byte[] mask = null;
            if (masked) {
                mask = in.readNBytes(4);
            }
            byte[] payload = in.readNBytes((int) length);
            if (masked && payload.length > 0) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i % 4];
                }
            }
            return new Frame(opcode, payload);
        }
    }
}