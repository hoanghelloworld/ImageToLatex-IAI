package com.example.image2latex.chatbot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private ImageView backButton;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private ChatAdapter adapter;
    private ChatbotHelper chatbotHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Ẩn thanh ActionBar vì chúng ta đã có toolbar tùy chỉnh
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Khởi tạo các thành phần giao diện
        recyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.edit_message);
        backButton = findViewById(R.id.back_button);
        sendButton = findViewById(R.id.button_send);
        progressBar = findViewById(R.id.progress_bar);
        
        // Thiết lập màu nền và màu chữ
        messageInput.setHint("Nhập tin nhắn của bạn...");
        
        // Thiết lập RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        // Tùy chỉnh adapter để sử dụng các bubble chat mới
        adapter = new ChatAdapter();
        adapter.setUserMessageBackground(R.drawable.chat_user_bubble);
        adapter.setBotMessageBackground(R.drawable.chat_bot_bubble);
        recyclerView.setAdapter(adapter);
        
        // Khởi tạo chatbot helper
        chatbotHelper = ChatbotHelperSingleton.getInstance(this);
        
        // Tải lịch sử chat
        loadChatHistory();
        
        // Thêm tin nhắn chào mừng nếu không có lịch sử
        if (adapter.getItemCount() == 0) {
            addWelcomeMessage();
        }
        
        // Thiết lập sự kiện nút quay lại
        backButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_click));
            onBackPressed();
        });
        
        // Thiết lập sự kiện nút gửi
        sendButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_click));
            sendMessage();
        });
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
            "Chào mừng bạn đến với Trợ Lý EduVisionEduVision! Bạn muốn tôi giúp gì hôm nay?", 
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
        
        // Thêm tin nhắn của người dùng vào chat
        ChatMessage userMessage = new ChatMessage(message, ChatMessage.TYPE_USER);
        adapter.addMessage(userMessage);
        
        // Xóa input và cuộn xuống dưới
        messageInput.setText("");
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        
        // Hiển thị trạng thái đang tải
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        
        // Gửi tin nhắn đến chatbot
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
                
                Toast.makeText(ChatActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                
                ChatMessage errorMessage = new ChatMessage(
                    "Xin lỗi, tôi gặp lỗi. Vui lòng thử lại.", 
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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}