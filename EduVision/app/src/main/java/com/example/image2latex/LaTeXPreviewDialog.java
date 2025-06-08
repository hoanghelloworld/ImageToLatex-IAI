package com.example.image2latex;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class LaTeXPreviewDialog extends Dialog {
    
    private final String latexCode;
    private TextView errorText;
    private WebView webView;
    
    public LaTeXPreviewDialog(@NonNull Context context, String latexCode) {
        super(context);
        this.latexCode = latexCode;
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_latex_preview);
        
        // Thiết lập hiệu ứng chuyển đổi cho dialog
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);
        
        webView = findViewById(R.id.preview_web_view);
        errorText = findViewById(R.id.error_text);
        Button closeButton = findViewById(R.id.close_button);
        
        closeButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.button_click));
            dismiss();
        });
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        renderLatex();
    }
    
    private void renderLatex() {
        try {
            String escapedLatex = latexCode.replace("\\", "\\\\")
                                         .replace("\"", "\\\"")
                                         .replace("\n", "\\n");
            
            String html = "<!DOCTYPE html><html><head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css\">"
                + "<script src=\"https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js\"></script>"
                + "<style>body{margin:16px;background:#fff;font-family:Arial,sans-serif;}#formula{font-size:18px}</style>"
                + "</head><body>"
                + "<div id=\"formula\"></div>"
                + "<script>"
                + "try{"
                + "  katex.render('" + escapedLatex + "', document.getElementById('formula'), {"
                + "    displayMode: true,"
                + "    throwOnError: false"
                + "  });"
                + "}catch(e){document.body.innerHTML='Lỗi: '+e.message;}"
                + "</script></body></html>";
            
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            webView.setVisibility(View.VISIBLE);
            errorText.setVisibility(View.GONE);
        } catch (Exception e) {
            webView.setVisibility(View.GONE);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("Lỗi khi hiển thị LaTeX: " + e.getMessage());
        }
    }
}