package com.example.image2latex.chatbot;

import android.content.Context;

public class ChatbotHelperSingleton {
    private static ChatbotHelper instance;
    
    private ChatbotHelperSingleton() {
        // Private constructor to prevent instantiation
    }
    
    public static synchronized ChatbotHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ChatbotHelper(context);
        }
        return instance;
    }
}