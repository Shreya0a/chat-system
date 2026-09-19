package com.lanmessenger.model;

/**
 * Minimal JSON encode/decode for {@link Message} objects, used on the
 * WebSocket wire. Avoids an external JSON library; messages are flat so a
 * tiny hand-rolled encoder is plenty.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toJson(Message message) {
        return "{\"type\":\"" + message.getType()
                + "\",\"sender\":\"" + escape(message.getSender())
                + "\",\"recipient\":\"" + escape(message.getRecipient())
                + "\",\"timestamp\":" + message.getTimestamp()
                + ",\"content\":\"" + escape(message.getContent())
                + "\"}";
    }

    public static Message fromJson(String json) {
        String type = null;
        String sender = "";
        String recipient = "";
        String content = "";
        long timestamp = 0L;

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && (json.charAt(i) == '{' || json.charAt(i) == ',' || json.charAt(i) == ' ' || json.charAt(i) == '}')) {
                i++;
            }
            if (i >= json.length()) {
                break;
            }
            if (json.charAt(i) != '"') {
                i++;
                continue;
            }
            int keyStart = ++i;
            int keyEnd = i;
            while (json.charAt(keyEnd) != '"') {
                keyEnd++;
            }
            String key = json.substring(keyStart, keyEnd);
            i = keyEnd + 1;
            while (i < json.length() && json.charAt(i) != ':') {
                i++;
            }
            i++;
            while (i < json.length() && json.charAt(i) == ' ') {
                i++;
            }
            if (i < json.length() && json.charAt(i) == '"') {
                i++;
                StringBuilder value = new StringBuilder();
                while (i < json.length()) {
                    char c = json.charAt(i);
                    if (c == '\\' && i + 1 < json.length()) {
                        char n = json.charAt(i + 1);
                        switch (n) {
                            case 'n': value.append('\n'); break;
                            case 'r': value.append('\r'); break;
                            case 't': value.append('\t'); break;
                            case '"': value.append('"'); break;
                            case '\\': value.append('\\'); break;
                            default: value.append(n);
                        }
                        i += 2;
                    } else if (c == '"') {
                        i++;
                        break;
                    } else {
                        value.append(c);
                        i++;
                    }
                }
                switch (key) {
                    case "type": type = value.toString(); break;
                    case "sender": sender = value.toString(); break;
                    case "recipient": recipient = value.toString(); break;
                    case "content": content = value.toString(); break;
                    default: break;
                }
            } else {
                int end = i;
                while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                    end++;
                }
                if (key.equals("timestamp")) {
                    timestamp = Long.parseLong(json.substring(i, end).trim());
                }
                i = end;
            }
        }
        return new Message(Message.Type.valueOf(type), sender, recipient, timestamp, content);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}