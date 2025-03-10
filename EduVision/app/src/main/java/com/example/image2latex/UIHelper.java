package com.example.image2latex.utils;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.example.image2latex.databinding.ActivityMainBinding;
import com.example.image2latex.LaTeXPreviewDialog;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UIHelper {
    // UI initialization and state management methods
    public static void initializeUI(ActivityMainBinding binding) {
        binding.galleryButton.setEnabled(false);
        binding.cameraButton.setEnabled(false);
        binding.cancelButton.setVisibility(View.GONE);
        binding.copyButton.setVisibility(View.GONE);
        binding.pasteButton.setVisibility(View.VISIBLE);
        binding.previewButton.setVisibility(View.GONE);
    }

    public static void enableButtons(ActivityMainBinding binding) {
        binding.galleryButton.setEnabled(true);
        binding.cameraButton.setEnabled(true);
    }

    public static void updateButtonVisibility(ActivityMainBinding binding, boolean hasText) {
        binding.copyButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
        binding.previewButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
        binding.pasteButton.setVisibility(hasText ? View.GONE : View.VISIBLE);
    }

    public static void updateUIState(ActivityMainBinding binding, boolean enabled) {
        binding.galleryButton.setEnabled(enabled);
        binding.cameraButton.setEnabled(enabled);
        binding.cancelButton.setVisibility(enabled ? View.GONE : View.VISIBLE);
        binding.copyButton.setVisibility(enabled && !binding.resultText.getText().toString().isEmpty() ? 
            View.VISIBLE : View.GONE);
        binding.previewButton.setVisibility(enabled && !binding.resultText.getText().toString().isEmpty() ? 
            View.VISIBLE : View.GONE);
    }

    // Clipboard handling methods
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("LaTeX Code", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "LaTeX code copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    public static void pasteFromClipboard(Context context, ActivityMainBinding binding) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence pastedText = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (pastedText != null) {
                binding.resultText.setText(pastedText.toString());
                Toast.makeText(context, "Text pasted from clipboard", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "No text to paste", Toast.LENGTH_SHORT).show();
        }
    }

    // Preview dialog method
    public static void showPreview(Context context, String latexCode) {
        if (!latexCode.isEmpty()) {
            LaTeXPreviewDialog previewDialog = new LaTeXPreviewDialog(context, latexCode);
            previewDialog.show();
        } else {
            Toast.makeText(context, "No LaTeX code to preview", Toast.LENGTH_SHORT).show();
        }
    }

    // Camera handling methods (merged from CameraUtils)
    public static File createImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        );
    }

    public static Intent createCameraIntent(Context context, Uri cameraImageUri) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        return intent;
    }

    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(
            context,
            context.getApplicationContext().getPackageName() + ".fileprovider",
            file
        );
    }
}