package com.example.image2latex.chatbot;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
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
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY = "AIzaSyBlU03Brw27Pdt_f85FyUVwvTbYRj-cfA8"; // Replace with your actual API key in production
    
    private final OkHttpClient client;
    private final Handler mainHandler;
    
    // Store conversation history
    private List<ChatMessage> conversationHistory = new ArrayList<>();
    
    public interface ChatResponseListener {
        void onResponse(String message);
        void onError(String error);
    }
    
    public ChatbotHelper() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
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
            String requestBody = buildRequestBody(message);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, requestBody);
            
            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + API_KEY)
                    .post(body)
                    .build();
                    
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
                    
                    String parsedResponse = parseResponse(responseData);
                    
                    // Add bot response to history
                    ChatMessage botResponse = new ChatMessage(parsedResponse, ChatMessage.TYPE_BOT);
                    addMessageToHistory(botResponse);
                    
                    mainHandler.post(() -> listener.onResponse(parsedResponse));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            listener.onError("Error: " + e.getMessage());
        }
    }
    
    private String buildRequestBody(String newMessage) {
        try {
            JSONObject json = new JSONObject();
            
            // Create contents array with conversation history
            JSONArray contents = new JSONArray();
            
            // Include instructions as first user message if this is a new conversation
            if (conversationHistory.size() <= 1) {
                JSONObject instructionContent = new JSONObject();
                JSONArray instructionParts = new JSONArray();
                JSONObject instructionPart = new JSONObject();
                
                instructionPart.put("text", "You are a helpful AI assistant specialized in LaTeX and mathematical equations. " +
                                    "Your answers should be clear and focused on helping users convert mathematical expressions to LaTeX. " +
                                    "For complex equations, provide step-by-step explanations when needed.");
                instructionParts.put(instructionPart);
                instructionContent.put("role", "user");
                instructionContent.put("parts", instructionParts);
                contents.put(instructionContent);
                
                // Add model's acknowledgment
                JSONObject ackContent = new JSONObject();
                JSONArray ackParts = new JSONArray();
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "I'll help you with LaTeX and mathematical equations. What can I assist you with today?");
                ackParts.put(ackPart);
                ackContent.put("role", "model");
                ackContent.put("parts", ackParts);
                contents.put(ackContent);
            }
            
            // Add previous conversation for context (limited to avoid token limits)
            int historyStartIdx = Math.max(0, conversationHistory.size() - 10);
            for (int i = historyStartIdx; i < conversationHistory.size(); i++) {
                ChatMessage msg = conversationHistory.get(i);
                JSONObject msgContent = new JSONObject();
                JSONArray msgParts = new JSONArray();
                JSONObject msgPart = new JSONObject();
                
                msgPart.put("text", msg.getMessage());
                msgParts.put(msgPart);
                msgContent.put("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "model");
                msgContent.put("parts", msgParts);
                contents.put(msgContent);
            }
            
            json.put("contents", contents);
            json.put("generationConfig", new JSONObject()
                    .put("temperature", 0.4)
                    .put("maxOutputTokens", 2048));
            
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error building request body", e);
            return "{}";
        }
    }
    
    private String parseResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            JSONObject firstPart = parts.getJSONObject(0);
            return firstPart.getString("text");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            return "Sorry, I couldn't process that response.";
        }
    }
    
    public void clearHistory() {
        conversationHistory.clear();
    }
}