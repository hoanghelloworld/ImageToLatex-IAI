package com.example.image2latex.documentwriter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.image2latex.R;
import com.example.image2latex.utils.ApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlRendererActivity extends AppCompatActivity {

    private static final String TAG = "HtmlRendererActivity";
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText;
    private Toolbar toolbar;
    
    private String documentContent;
    private String documentId;
    
    private ApiClient apiClient;
    
    private Executor executor = Executors.newSingleThreadExecutor();
    
    // Regex pattern to identify LaTeX formulas wrapped in $...$ or $$...$$
    private static final Pattern LATEX_PATTERN = Pattern.compile("\\$\\$(.*?)\\$\\$|\\$(.*?)\\$", Pattern.DOTALL);

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_renderer);
        
        // Initialize API client
        apiClient = new ApiClient(this);
        
        // Initialize views
        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        toolbar = findViewById(R.id.toolbar);
        
        // Set up toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("LaTeX HTML Renderer");
            }
        }
        
        // Get data from intent
        documentContent = getIntent().getStringExtra("document_content");
        documentId = getIntent().getStringExtra("document_id");
        
        // Set up WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // Render the document
        renderDocument(documentContent);
    }
    
    private void renderDocument(String documentContent) {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        
        // Execute in background
        new Thread(() -> {
            try {
                // Send the full document to the server instead of just extracting formulas
                String fullLatexContent = extractLatexContent(documentContent, false);
                
                // Log the content being sent
                Log.d(TAG, "Sending full document to server with length: " + fullLatexContent.length());
                
                // Call API to render LaTeX to HTML
                JSONObject response = apiClient.renderLatexToHtml(fullLatexContent);
                
                if (response.has("html")) {
                    String html = response.getString("html");
                    String css = response.has("css") ? response.getString("css") : "";
                    
                    // Display the rendered content in the WebView
                    runOnUiThread(() -> {
                        displayRenderedContent(html, css);
                    });
                } else {
                    runOnUiThread(() -> {
                        showError("Invalid response from server");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rendering document", e);
                final String errorMessage = e.getMessage();
                
                runOnUiThread(() -> {
                    showError("Error: " + errorMessage);
                });
            }
        }).start();
    }
    
    /**
     * Extract LaTeX content from the document
     * @param text The document text
     * @param extractFormulasOnly If true, extracts only formulas; if false, returns the full document
     * @return LaTeX content
     */
    private String extractLatexContent(String text, boolean extractFormulasOnly) {
        // If we want the full document, just return it as is
        if (!extractFormulasOnly) {
            return text;
        }
        
        // Extract formulas only (original behavior)
        List<String> formulas = new ArrayList<>();
        Matcher matcher = LATEX_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String formula = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (formula != null && !formula.trim().isEmpty()) {
                formulas.add(formula.trim());
            }
        }
        
        // Return the first formula if only one is found
        if (formulas.size() == 1) {
            return formulas.get(0);
        }
        
        // Combine multiple formulas with line breaks if more than one is found
        if (!formulas.isEmpty()) {
            StringBuilder combined = new StringBuilder();
            for (String formula : formulas) {
                if (combined.length() > 0) {
                    combined.append("\\\\");  // Double backslash for new line in LaTeX
                }
                combined.append(formula);
            }
            return combined.toString();
        }
        
        return "";
    }
    
    // Maintain backward compatibility with existing code
    private String extractLatexContent(String text) {
        return extractLatexContent(text, true);
    }
    
    private void showError(final String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(message);
            Toast.makeText(HtmlRendererActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Display rendered HTML and CSS in the WebView
     */
    private void displayRenderedContent(String html, String css) {
        // Set up WebView to intercept CSS file requests
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.d(TAG, "Intercepting request for: " + url);
                
                // Extract the filename from the URL
                String filename = url.substring(url.lastIndexOf('/') + 1);
                
                // Intercept the CSS file request
                if (filename.equals("try.css") || url.endsWith(".css")) {
                    Log.d(TAG, "Serving CSS content");
                    // Provide the CSS content directly
                    return new WebResourceResponse(
                        "text/css", 
                        "UTF-8",
                        new ByteArrayInputStream(css.getBytes())
                    );
                }
                
                // For images, we would need to request them from the server
                if (filename.matches("try\\d+x\\.png")) {
                    Log.d(TAG, "Image requested: " + filename);
                    
                    try {
                        // Fetch the image from the server
                        byte[] imageData = apiClient.fetchImage(filename);
                        Log.d(TAG, "Image loaded: " + filename + " (" + imageData.length + " bytes)");
                        
                        // Return the image data
                        return new WebResourceResponse(
                            "image/png", 
                            "UTF-8",
                            new ByteArrayInputStream(imageData)
                        );
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image: " + filename, e);
                    }
                }
                
                return super.shouldInterceptRequest(view, url);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });
        
        // Load the HTML with a base URL to resolve relative paths
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
    }
} 