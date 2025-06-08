package com.example.image2latex.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.FileProvider;
import androidx.viewbinding.ViewBinding;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.example.image2latex.LaTeXPreviewDialog;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UIHelper {
    // UI initialization and state management methods
    public static void initializeUI(ViewBinding binding) {
        // Sử dụng findViewById thay vì truy cập trực tiếp
        View rootView = binding.getRoot();
        Button galleryButton = rootView.findViewById(com.example.image2latex.R.id.galleryButton);
        Button cameraButton = rootView.findViewById(com.example.image2latex.R.id.cameraButton);
        View cancelButton = rootView.findViewById(com.example.image2latex.R.id.cancelButton);
        View loadingProgressBar = rootView.findViewById(com.example.image2latex.R.id.loadingProgressBar);
        Button previewButton = rootView.findViewById(com.example.image2latex.R.id.previewButton);
        Button copyButton = rootView.findViewById(com.example.image2latex.R.id.copyButton);
        Button pasteButton = rootView.findViewById(com.example.image2latex.R.id.pasteButton);
        
        // Ẩn các button khi mới khởi động ứng dụng
        if (cancelButton != null) cancelButton.setVisibility(View.GONE);
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE);
        if (previewButton != null) previewButton.setVisibility(View.GONE);
        if (copyButton != null) copyButton.setVisibility(View.GONE);
        
        // Kích hoạt các button
        if (galleryButton != null) galleryButton.setEnabled(true);
        if (cameraButton != null) cameraButton.setEnabled(true);
        if (pasteButton != null) pasteButton.setEnabled(true);
    }

    public static void enableButtons(ViewBinding binding) {
        // Truy cập các button và cập nhật trạng thái
        View rootView = binding.getRoot();
        Button galleryButton = rootView.findViewById(com.example.image2latex.R.id.galleryButton);
        Button cameraButton = rootView.findViewById(com.example.image2latex.R.id.cameraButton);
        Button pasteButton = rootView.findViewById(com.example.image2latex.R.id.pasteButton);
        
        if (galleryButton != null) galleryButton.setEnabled(true);
        if (cameraButton != null) cameraButton.setEnabled(true);
        if (pasteButton != null) pasteButton.setEnabled(true);
    }

    public static void updateButtonVisibility(ViewBinding binding, boolean hasText) {
        // Cập nhật hiển thị của các button dựa trên trạng thái text
        View rootView = binding.getRoot();
        Button previewButton = rootView.findViewById(com.example.image2latex.R.id.previewButton);
        Button copyButton = rootView.findViewById(com.example.image2latex.R.id.copyButton);
        
        if (previewButton != null) previewButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
        if (copyButton != null) copyButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }

    public static void updateUIState(ViewBinding binding, boolean enabled) {
        // Cập nhật trạng thái UI khi đang xử lý hoặc hoàn thành
        View rootView = binding.getRoot();
        Button galleryButton = rootView.findViewById(com.example.image2latex.R.id.galleryButton);
        Button cameraButton = rootView.findViewById(com.example.image2latex.R.id.cameraButton);
        Button pasteButton = rootView.findViewById(com.example.image2latex.R.id.pasteButton);
        View cancelButton = rootView.findViewById(com.example.image2latex.R.id.cancelButton);
        View loadingProgressBar = rootView.findViewById(com.example.image2latex.R.id.loadingProgressBar);
        ImageView imageView = rootView.findViewById(com.example.image2latex.R.id.imageView);
        
        if (galleryButton != null) galleryButton.setEnabled(enabled);
        if (cameraButton != null) cameraButton.setEnabled(enabled);
        if (pasteButton != null) pasteButton.setEnabled(enabled);
        if (cancelButton != null) cancelButton.setVisibility(enabled ? View.GONE : View.VISIBLE);
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(enabled ? View.GONE : View.VISIBLE);
        
        if (imageView != null) {
            imageView.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    // Clipboard handling methods
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("LaTeX Code", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Đã sao chép mã LaTeX vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
    }

    public static void pasteFromClipboard(Context context, ViewBinding binding) {
        View rootView = binding.getRoot();
        TextView resultText = rootView.findViewById(com.example.image2latex.R.id.resultText);
        if (resultText == null) return;
        
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence pastedText = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (pastedText != null) {
                resultText.setText(pastedText.toString());
                Toast.makeText(context, "Đã dán văn bản từ bộ nhớ tạm", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Không có văn bản để dán", Toast.LENGTH_SHORT).show();
        }
    }

    // Preview dialog method
    public static void showPreview(Context context, String latexCode) {
        if (!latexCode.isEmpty()) {
            LaTeXPreviewDialog previewDialog = new LaTeXPreviewDialog(context, latexCode);
            previewDialog.show();
        } else {
            Toast.makeText(context, "Không có mã LaTeX để xem trước", Toast.LENGTH_SHORT).show();
        }
    }

    // Hiệu ứng chuyển đổi màn hình
    public static void startActivity(Activity activity, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(activity);
            activity.startActivity(intent, options.toBundle());
        } else {
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    // Hiệu ứng hiển thị và ẩn view với animation
    public static void showViewWithAnimation(final View view) {
        if (view.getVisibility() == View.VISIBLE) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = view.getWidth() / 2;
            int cy = view.getHeight() / 2;
            float finalRadius = (float) Math.hypot(cx, cy);
            
            Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            anim.setDuration(300);
            anim.setInterpolator(new DecelerateInterpolator());
            
            view.setVisibility(View.VISIBLE);
            anim.start();
        } else {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
    
    public static void hideViewWithAnimation(final View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = view.getWidth() / 2;
            int cy = view.getHeight() / 2;
            float initialRadius = (float) Math.hypot(cx, cy);
            
            Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);
            anim.setDuration(300);
            anim.setInterpolator(new AccelerateInterpolator());
            
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.GONE);
                }
            });
            
            anim.start();
        } else {
            view.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.GONE);
                        }
                    })
                    .start();
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