package com.example.image2latex.chatbot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.image2latex.utils.ServerConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotHelper {
    private static final String TAG = "ChatbotHelper";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Handler mainHandler;
    private final ServerConfig serverConfig;
    
    // Store conversation history
    private List<ChatMessage> conversationHistory = new ArrayList<>();
    
    public interface ChatResponseListener {
        void onResponse(String message);
        void onError(String error);
    }
    
    public ChatbotHelper(Context context) {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
        serverConfig = ServerConfig.getInstance(context);
    }
    
    public List<ChatMessage> getConversationHistory() {
        return conversationHistory;
    }
    
    public void addMessageToHistory(ChatMessage message) {
        if (!containsMessage(message)) {
            conversationHistory.add(message);
        }
    }
    
    private boolean containsMessage(ChatMessage newMessage) {
        for (ChatMessage message : conversationHistory) {
            if (message.getMessage().equals(newMessage.getMessage()) && 
                message.getType() == newMessage.getType() &&
                Math.abs(message.getTimestamp() - newMessage.getTimestamp()) < 1000) {
                return true;
            }
        }
        return false;
    }
    
    public void sendMessage(String message, ChatResponseListener listener) {
        ChatMessage userMessage = new ChatMessage(message, ChatMessage.TYPE_USER);
        addMessageToHistory(userMessage);
        
        try {
            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("message", message);
            
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(serverConfig.getChatbotApiUrl())
                    .post(body)
                    .build();
                    
            Log.d(TAG, "Sending request to: " + serverConfig.getChatbotApiUrl());
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    mainHandler.post(() -> listener.onError("Network error: " + e.getMessage()));
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API error: " + response.code() + " - " + responseData);
                        mainHandler.post(() -> listener.onError("API error: " + response.code()));
                        return;
                    }
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String parsedResponse = jsonResponse.getString("response");
                        
                        // Add bot response to history
                        ChatMessage botResponse = new ChatMessage(parsedResponse, ChatMessage.TYPE_BOT);
                        addMessageToHistory(botResponse);
                        
                        mainHandler.post(() -> listener.onResponse(parsedResponse));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        mainHandler.post(() -> listener.onError("Error parsing response: " + e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            listener.onError("Error: " + e.getMessage());
        }
    }
    
    public void clearHistory() {
        conversationHistory.clear();
    }
}