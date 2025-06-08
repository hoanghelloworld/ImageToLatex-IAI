package com.example.image2latex.chatbot;

/**
 * Represents a chat message in the conversation
 */
public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    
    private String text;
    private int type;
    private long timestamp;
    
    /**
     * Constructor for creating a new chat message
     * 
     * @param text The text content of the message
     * @param type The type of message (TYPE_USER or TYPE_BOT)
     */
    public ChatMessage(String text, int type) {
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the text content of the message
     * 
     * @return The message text
     */
    public String getText() {
        return text;
    }
    
    /**
     * For compatibility with existing code - same as getText()
     * 
     * @return The message text
     */
    public String getMessage() {
        return text;
    }
    
    /**
     * Set the text content of the message
     * 
     * @param text The new message text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Set the message content - for compatibility
     * 
     * @param message The new message text
     */
    public void setMessage(String message) {
        this.text = message;
    }
    
    /**
     * Get the type of the message
     * 
     * @return The message type (TYPE_USER or TYPE_BOT)
     */
    public int getType() {
        return type;
    }
    
    /**
     * Set the type of the message
     * 
     * @param type The new message type
     */
    public void setType(int type) {
        this.type = type;
    }
    
    /**
     * Get the timestamp when the message was created
     * 
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the timestamp of the message
     * 
     * @param timestamp The new timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}