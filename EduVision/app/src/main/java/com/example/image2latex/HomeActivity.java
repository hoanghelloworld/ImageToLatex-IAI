package com.example.image2latex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.image2latex.chatbot.ChatActivity;
import com.example.image2latex.documentwriter.DocumentListActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Set up converter card click
        CardView conversionCard = findViewById(R.id.conversionCard);
        Button openConverterButton = findViewById(R.id.openConverterButton);
        
        View.OnClickListener openConverterListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ConversionActivity.class);
                startActivity(intent);
            }
        };
        
        conversionCard.setOnClickListener(openConverterListener);
        openConverterButton.setOnClickListener(openConverterListener);
        
        // Set up chat card click
        CardView chatCard = findViewById(R.id.chatCard);
        Button openChatButton = findViewById(R.id.openChatButton);
        
        View.OnClickListener openChatListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        };
        
        chatCard.setOnClickListener(openChatListener);
        openChatButton.setOnClickListener(openChatListener);
        
        // Set up document writer card click
        CardView documentCard = findViewById(R.id.documentCard);
        Button openDocumentButton = findViewById(R.id.openDocumentButton);
        
        View.OnClickListener openDocumentListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, DocumentListActivity.class);
                startActivity(intent);
            }
        };
        
        documentCard.setOnClickListener(openDocumentListener);
        openDocumentButton.setOnClickListener(openDocumentListener);
    }
} 