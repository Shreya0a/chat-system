package com.lanmessenger.model;

/**
 * A single chat message plus the wire protocol used to transport it.
 *
 * Messages travel over the wire as a single line:
 *
 *   TYPE\tsender\trecipient\ttimestamp\tcontent
 *
 * `content` is escaped so that tabs, newlines and backslashes inside the text
 * survive the round trip.
 */
public class Message {

    public enum Type {
        JOIN,      // client announces its username (first message on connect)
        LEAVE,     // client says goodbye / detected disconnect
        PUBLIC,    // visible to every connected client
        PRIVATE,   // addressed to exactly one recipient
        SERVER,    // server-generated info such as join/leave notices
        HISTORY,   // buffered session history sent to a fresh client
        USERLIST   // list of currently online users
    }

    private final Type type;
    private final String sender;
    private final String recipient;
    private final long timestamp;
    private final String content;

    public Message(Type type, String sender, String recipient, long timestamp, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = timestamp;
        this.content = content;
    }

    public static Message now(Type type, String sender, String recipient, String content) {
        return new Message(type, sender, recipient, System.currentTimeMillis(), content);
    }

    public Type getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }

    /** Encode this message into the one-line wire format. */
    public String serialize() {
        return type.name() + "\t" + nullSafe(sender) + "\t" + nullSafe(recipient)
                + "\t" + timestamp + "\t" + escape(content);
    }

    /** Decode a wire-format line back into a Message. */
    public static Message deserialize(String line) {
        String[] parts = line.split("\t", 5);
        Type type = Type.valueOf(parts[0]);
        String sender = parts.length > 1 ? unescape(parts[1]) : "";
        String recipient = parts.length > 2 ? unescape(parts[2]) : "";
        long ts = parts.length > 3 && !parts[3].isEmpty() ? Long.parseLong(parts[3]) : 0L;
        String content = parts.length > 4 ? unescape(parts[4]) : "";
        return new Message(type, sender, recipient, ts, content);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                if (next == 't') {
                    out.append('\t');
                } else if (next == 'n') {
                    out.append('\n');
                } else {
                    out.append(next);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}