package com.example.image2latex.chatbot;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    
    private String message;
    private int type;
    private long timestamp;
    
    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getType() {
        return type;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}