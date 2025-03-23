package com.example.image2latex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Set up the camera/gallery feature card
        CardView conversionCard = findViewById(R.id.conversionCard);
        Button btnConvertImage = findViewById(R.id.btnConvertImage);
        
        conversionCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open camera/gallery activity
                startConversionActivity();
            }
        });
        
        btnConvertImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open camera/gallery activity
                startConversionActivity();
            }
        });
        
        // Set up chat feature card
        CardView chatCard = findViewById(R.id.chatCard);
        Button btnStartChat = findViewById(R.id.btnStartChat);
        
        chatCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open chat activity
                startChatActivity();
            }
        });
        
        btnStartChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open chat activity
                startChatActivity();
            }
        });
    }
    
    private void startConversionActivity() {
        startActivity(new Intent(this, ConversionActivity.class));
    }
    
    private void startChatActivity() {
        // Start the ChatActivity from the chatbot package
        startActivity(new Intent(this, com.example.image2latex.chatbot.ChatActivity.class));
    }
}