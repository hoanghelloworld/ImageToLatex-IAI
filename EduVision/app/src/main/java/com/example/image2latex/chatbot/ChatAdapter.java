package com.example.image2latex.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.image2latex.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private List<ChatMessage> messages = new ArrayList<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        String formattedTime = timeFormat.format(new Date(message.getTimestamp()));
        
        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.messageText.setText(message.getMessage());
            userHolder.timeText.setText(formattedTime);
            userHolder.messageText.setTextColor(0xFF000000); // Black text
        } else if (holder instanceof BotMessageViewHolder) {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            botHolder.messageText.setText(message.getMessage());
            botHolder.timeText.setText(formattedTime);
            botHolder.messageText.setTextColor(0xFF000000); // Black text
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        
        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_user);
            timeText = itemView.findViewById(R.id.text_time_user);
        }
    }
    
    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        
        BotMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_bot);
            timeText = itemView.findViewById(R.id.text_time_bot);
        }
    }
}