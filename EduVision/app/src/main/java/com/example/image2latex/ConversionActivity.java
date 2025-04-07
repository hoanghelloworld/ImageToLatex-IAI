package com.example.image2latex;

import java.io.File;
import java.io.IOException;
import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.image2latex.databinding.ActivityConversionBinding;
import com.example.image2latex.chatbot.ChatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Import utility classes from the project
import com.example.image2latex.utils.UIHelper;
import com.example.image2latex.utils.PermissionManager;
import com.example.image2latex.utils.ImageHandler;
import com.example.image2latex.LaTeXConverter;

public class ConversionActivity extends AppCompatActivity {
    private ActivityConversionBinding binding;
    private LaTeXConverter converter;
    private Thread conversionThread = null;
    private Uri cameraImageUri = null;
    private FloatingActionButton chatFab;
    
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int GALLERY_REQUEST_CODE = 103;
    private static final int UCROP_REQUEST_CODE = 104;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Enable up navigation
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image to LaTeX");
        }
        
        UIHelper.initializeUI(binding);
        initializeConverter();
        setupButtonListeners();
        setupTextWatcher();
        
        chatFab = binding.chatFab;
        chatFab.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            startActivity(chatIntent);
        });
    }
    
    private void initializeConverter() {
        try {
            // Initialize converter synchronously - no need for async initialization since
            // we're not loading large models anymore
            converter = new LaTeXConverter(this);
            UIHelper.enableButtons(binding);
        } catch (Exception e) {
            handleConverterInitError(e);
        }
    }

    private void setupButtonListeners() {
        binding.galleryButton.setOnClickListener(v -> {
            if (PermissionManager.checkStoragePermission(this)) {
                openGallery();
            } else {
                PermissionManager.checkPermission(this, PermissionManager.getStoragePermission(), STORAGE_PERMISSION_CODE);
            }
        });
        
        binding.cameraButton.setOnClickListener(v -> {
            if (PermissionManager.checkCameraPermission(this)) {
                openCamera();
            } else {
                PermissionManager.checkPermission(this, Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
            }
        });
        
        binding.cancelButton.setOnClickListener(v -> {
            if (converter != null && conversionThread != null && conversionThread.isAlive()) {
                converter.cancelConversion();
                Toast.makeText(this, "Cancelling...", Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.previewButton.setOnClickListener(v -> 
            UIHelper.showPreview(this, binding.resultText.getText().toString()));
        
        binding.copyButton.setOnClickListener(v -> {
            String latexCode = binding.resultText.getText().toString();
            if (!latexCode.isEmpty()) {
                UIHelper.copyToClipboard(this, latexCode);
            }
        });

        binding.pasteButton.setOnClickListener(v -> 
            UIHelper.pasteFromClipboard(this, binding));
    }
    
    private void setupTextWatcher() {
        binding.resultText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                UIHelper.updateButtonVisibility(binding, s.length() > 0);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void openCamera() {
        try {
            File photoFile = UIHelper.createImageFile(this);
            cameraImageUri = UIHelper.getUriForFile(this, photoFile);
            startActivityForResult(UIHelper.createCameraIntent(this, cameraImageUri), 
                CAMERA_REQUEST_CODE);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.imageView.setImageBitmap(bitmap);
        binding.resultText.setText("Processing image...");
        UIHelper.updateUIState(binding, false);
        
        conversionThread = new Thread(() -> {
            try {
                // Pass the bitmap directly to the converter instead of preprocessing it
                String result = converter.convert(bitmap);
                runOnUiThread(() -> updateUIWithResult(result));
            } catch (Exception e) {
                runOnUiThread(() -> handleProcessingError(e));
            }
        });
        conversionThread.start();
    }

    private void updateUIWithResult(String result) {
        binding.resultText.setText(result);
        UIHelper.updateUIState(binding, true);
    }

    private void handleProcessingError(Exception e) {
        binding.resultText.setText("Error: " + e.getMessage());
        UIHelper.updateUIState(binding, true);
    }

    private void handleConverterInitError(Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error initializing: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ImageHandler.handleActivityResult(this, requestCode, resultCode, data, 
            binding, cameraImageUri, this::processImage);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handlePermissionResult(this, requestCode, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_chat) {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            startActivity(chatIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (converter != null) {
            converter.cancelConversion();
        }
        if (conversionThread != null && conversionThread.isAlive()) {
            conversionThread.interrupt();
        }
    }

    @Override
    public void onBackPressed() {
        if (conversionThread != null && conversionThread.isAlive() && 
            binding.cancelButton.getVisibility() == View.VISIBLE) {
            converter.cancelConversion();
            Toast.makeText(this, "Cancelling operation...", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
} 