package com.example.image2latex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.image2latex.utils.UIHelper;

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
                // Áp dụng hiệu ứng khi nhấn
                v.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_click));
                // Mở màn hình chuyển đổi ảnh với hiệu ứng
                startConversionActivity();
            }
        });
        
        btnConvertImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Áp dụng hiệu ứng khi nhấn
                v.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_click));
                // Mở màn hình chuyển đổi ảnh với hiệu ứng
                startConversionActivity();
            }
        });
        
        // Set up chat feature card
        CardView chatCard = findViewById(R.id.chatCard);
        Button btnStartChat = findViewById(R.id.btnStartChat);
        
        chatCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Áp dụng hiệu ứng khi nhấn
                v.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_click));
                // Mở màn hình chat với hiệu ứng
                startChatActivity();
            }
        });
        
        btnStartChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Áp dụng hiệu ứng khi nhấn
                v.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_click));
                // Mở màn hình chat với hiệu ứng
                startChatActivity();
            }
        });
    }
    
    private void startConversionActivity() {
        Intent intent = new Intent(this, ConversionActivity.class);
        UIHelper.startActivity(this, intent);
    }
    
    private void startChatActivity() {
        // Start the ChatActivity from the chatbot package
        Intent intent = new Intent(this, com.example.image2latex.chatbot.ChatActivity.class);
        UIHelper.startActivity(this, intent);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}