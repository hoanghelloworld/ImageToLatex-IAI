package com.example.image2latex.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.image2latex.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    
    private List<ChatMessage> messages = new ArrayList<>();
    private int userMessageBackground = R.drawable.chat_user_bubble;
    private int botMessageBackground = R.drawable.chat_bot_bubble;
    
    // Getter and Setter for message backgrounds
    public void setUserMessageBackground(int resId) {
        this.userMessageBackground = resId;
    }
    
    public void setBotMessageBackground(int resId) {
        this.botMessageBackground = resId;
    }
    
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == ChatMessage.TYPE_USER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
        }
        return new ChatViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.messageText.setText(message.getText());
        
        // Thiết lập background tùy chỉnh
        if (message.getType() == ChatMessage.TYPE_USER) {
            holder.messageContainer.setBackgroundResource(userMessageBackground);
            holder.messageText.setTextColor(0xFFFFFFFF); // Màu trắng cho tin nhắn người dùng
        } else {
            holder.messageContainer.setBackgroundResource(botMessageBackground);
            holder.messageText.setTextColor(0xFF212121); // Màu đen cho tin nhắn bot
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        View messageContainer;
        
        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            messageContainer = itemView.findViewById(R.id.message_container);
        }
    }
}