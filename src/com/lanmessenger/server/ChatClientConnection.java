package com.lanmessenger.server;

import com.lanmessenger.model.Message;

public interface ChatClientConnection {
    String getUsername();
    void setUsername(String username);
    void sendMessage(Message message);
    void close();
}