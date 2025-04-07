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
            
            // Include specific instructions based on the type of request
            if (newMessage.contains("Translate to ")) {
                JSONObject instructionContent = new JSONObject();
                JSONArray instructionParts = new JSONArray();
                JSONObject instructionPart = new JSONObject();
                
                instructionPart.put("text", "You are a translation assistant. When translating text, respond ONLY with the " +
                        "translated content without explanations or comments. Make the translation sound natural in the target language. " +
                        "If the source language isn't specified, detect it automatically and translate to the requested language.");
                instructionParts.put(instructionPart);
                instructionContent.put("role", "user");
                instructionContent.put("parts", instructionParts);
                contents.put(instructionContent);
                
                // Add model's acknowledgment
                JSONObject ackContent = new JSONObject();
                JSONArray ackParts = new JSONArray();
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "I understand. I'll provide only the translated text without explanations.");
                ackParts.put(ackPart);
                ackContent.put("role", "model");
                ackContent.put("parts", ackParts);
                contents.put(ackContent);
            } else if (newMessage.contains("Improve my writing") || newMessage.contains("Fix spelling") || 
                newMessage.contains("Make this shorter") || newMessage.contains("Make this longer") || 
                newMessage.contains("Change the tone")) {
                
                JSONObject instructionContent = new JSONObject();
                JSONArray instructionParts = new JSONArray();
                JSONObject instructionPart = new JSONObject();
                
                instructionPart.put("text", "You are an AI writing assistant helping with document editing. " +
                                 "When you receive text to improve, fix, translate, explain, shorten, lengthen, or change tone, " +
                                 "focus ONLY on the specific request. Return ONLY the modified text without additional explanations, " +
                                 "comments, or formatting. Your response will be directly inserted into the document.");
                instructionParts.put(instructionPart);
                instructionContent.put("role", "user");
                instructionContent.put("parts", instructionParts);
                contents.put(instructionContent);
                
                // Add model's acknowledgment
                JSONObject ackContent = new JSONObject();
                JSONArray ackParts = new JSONArray();
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "I understand. I'll process your text according to your request and return only the modified text.");
                ackParts.put(ackPart);
                ackContent.put("role", "model");
                ackContent.put("parts", ackParts);
                contents.put(ackContent);
            } else if (newMessage.contains("Explain this")) {
                JSONObject instructionContent = new JSONObject();
                JSONArray instructionParts = new JSONArray();
                JSONObject instructionPart = new JSONObject();
                
                instructionPart.put("text", "You are an AI educational assistant. When asked to explain text, provide a clear, " +
                                 "concise explanation that helps the user understand the content better. Break down complex concepts " +
                                 "and explain any technical terms. For mathematical expressions, explain the meaning and purpose.");
                instructionParts.put(instructionPart);
                instructionContent.put("role", "user");
                instructionContent.put("parts", instructionParts);
                contents.put(instructionContent);
                
                JSONObject ackContent = new JSONObject();
                JSONArray ackParts = new JSONArray();
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "I'll provide a clear and helpful explanation of the text.");
                ackParts.put(ackPart);
                ackContent.put("role", "model");
                ackContent.put("parts", ackParts);
                contents.put(ackContent);
            } else if (newMessage.contains("Reference Text:")) {
                // Handle the case where there's a custom query with reference text
                JSONObject instructionContent = new JSONObject();
                JSONArray instructionParts = new JSONArray();
                JSONObject instructionPart = new JSONObject();
                
                instructionPart.put("text", "You are an AI assistant helping with a document. The user will ask a question " +
                                 "and provide reference text from their document. When answering, focus on addressing the question " +
                                 "in relation to the provided reference text. Give a helpful, clear response.");
                instructionParts.put(instructionPart);
                instructionContent.put("role", "user");
                instructionContent.put("parts", instructionParts);
                contents.put(instructionContent);
                
                JSONObject ackContent = new JSONObject();
                JSONArray ackParts = new JSONArray();
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "I'll address your question in relation to the provided reference text.");
                ackParts.put(ackPart);
                ackContent.put("role", "model");
                ackContent.put("parts", ackParts);
                contents.put(ackContent);
            } else if (conversationHistory.size() <= 1) {
                // Standard chat instruction for other cases
                JSONObject instructionContent = new JSONObject();
                JSONArray instructionParts = new JSONArray();
                JSONObject instructionPart = new JSONObject();
                
                instructionPart.put("text", "You are a helpful AI assistant specialized in LaTeX and mathematical equations. " +
                                    "Your answers should be clear and focused on helping users with document writing and mathematical expressions.");
                instructionParts.put(instructionPart);
                instructionContent.put("role", "user");
                instructionContent.put("parts", instructionParts);
                contents.put(instructionContent);
                
                // Add model's acknowledgment
                JSONObject ackContent = new JSONObject();
                JSONArray ackParts = new JSONArray();
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "I'll help you with your documents and mathematical expressions. What can I assist you with today?");
                ackParts.put(ackPart);
                ackContent.put("role", "model");
                ackContent.put("parts", ackParts);
                contents.put(ackContent);
            }
            
            // Add previous conversation for context (limited to avoid token limits)
            int historyStartIdx = Math.max(0, conversationHistory.size() - 6);
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
            
            // Adjust temperature based on the type of request
            float temperature = 0.4f;
            if (newMessage.contains("Improve my writing") || newMessage.contains("Make this longer")) {
                temperature = 0.7f; // More creative for improving writing
            } else if (newMessage.contains("Fix spelling") || newMessage.contains("Translate to")) {
                temperature = 0.2f; // More precise for corrections and translations
            }
            
            json.put("generationConfig", new JSONObject()
                    .put("temperature", temperature)
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