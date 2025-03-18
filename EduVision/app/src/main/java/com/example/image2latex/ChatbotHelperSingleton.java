package com.example.image2latex.chatbot;

public class ChatbotHelperSingleton {
    private static ChatbotHelper instance;
    
    private ChatbotHelperSingleton() {
        // Private constructor to prevent instantiation
    }
    
    public static synchronized ChatbotHelper getInstance() {
        if (instance == null) {
            instance = new ChatbotHelper();
        }
        return instance;
    }
}