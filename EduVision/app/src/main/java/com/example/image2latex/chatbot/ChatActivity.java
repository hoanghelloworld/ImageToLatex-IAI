package com.example.image2latex.chatbot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.image2latex.R;

import java.util.List;

// Correct imports for chatbot package classes
import com.example.image2latex.chatbot.ChatMessage;
import com.example.image2latex.chatbot.ChatAdapter;
import com.example.image2latex.chatbot.ChatbotHelper;
import com.example.image2latex.chatbot.ChatbotHelperSingleton;

public class ChatActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private ChatAdapter adapter;
    private ChatbotHelper chatbotHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("LaTeX Assistant");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize UI elements
        recyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.button_send);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set background and text colors
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundColor(getResources().getColor(android.R.color.white));
        messageInput.setTextColor(getResources().getColor(android.R.color.black));
        messageInput.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        
        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatAdapter();
        recyclerView.setAdapter(adapter);
        
        // Get the chatbot helper instance
        chatbotHelper = ChatbotHelperSingleton.getInstance();
        
        // Load chat history
        loadChatHistory();
        
        // Add welcome message if there's no history
        if (adapter.getItemCount() == 0) {
            addWelcomeMessage();
        }
        
        // Set up send button listener
        sendButton.setOnClickListener(v -> sendMessage());
    }
    
    private void loadChatHistory() {
        List<ChatMessage> history = chatbotHelper.getConversationHistory();
        for (ChatMessage message : history) {
            adapter.addMessage(message);
        }
        if (adapter.getItemCount() > 0) {
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
    }
    
    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
            "Welcome to the LaTeX Assistant! I can help you convert mathematical expressions to LaTeX code. What would you like to convert today?", 
            ChatMessage.TYPE_BOT
        );
        adapter.addMessage(welcomeMessage);
        chatbotHelper.addMessageToHistory(welcomeMessage);
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }
        
        // Add user message to chat
        ChatMessage userMessage = new ChatMessage(message, ChatMessage.TYPE_USER);
        adapter.addMessage(userMessage);
        
        // Clear input and scroll to bottom
        messageInput.setText("");
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        
        // Send message to chatbot
        chatbotHelper.sendMessage(message, new ChatbotHelper.ChatResponseListener() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                
                ChatMessage botMessage = new ChatMessage(response, ChatMessage.TYPE_BOT);
                adapter.addMessage(botMessage);
                recyclerView.post(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1));
            }
            
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                
                Toast.makeText(ChatActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                
                ChatMessage errorMessage = new ChatMessage(
                    "Sorry, I encountered an error. Please try again.", 
                    ChatMessage.TYPE_BOT
                );
                adapter.addMessage(errorMessage);
                chatbotHelper.addMessageToHistory(errorMessage);
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 