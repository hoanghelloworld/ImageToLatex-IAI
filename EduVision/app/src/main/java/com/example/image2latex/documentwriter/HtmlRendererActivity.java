package com.example.image2latex.documentwriter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });
        
        // Render the document
        renderDocument();
    }
    
    private void renderDocument() {
        if (documentContent == null || documentContent.isEmpty()) {
            showError("Document content is empty");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        
        executor.execute(() -> {
            try {
                // Extract all LaTeX formulas from the document
                String latexContent = extractLatexContent(documentContent);
                
                if (latexContent.isEmpty()) {
                    runOnUiThread(() -> {
                        showError("No LaTeX formulas found in the document");
                    });
                    return;
                }
                
                Log.d(TAG, "Extracted LaTeX: " + latexContent);
                
                // Send the request to server
                String htmlResponse = apiClient.renderLatexToHtml(latexContent);
                
                // Display the HTML response
                runOnUiThread(() -> {
                    webView.loadDataWithBaseURL(null, htmlResponse, "text/html", "UTF-8", null);
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error rendering document", e);
                showError("Network error: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "JSON error rendering document", e);
                showError("JSON error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error rendering document", e);
                showError("Error rendering document: " + e.getMessage());
            }
        });
    }
    
    /**
     * Extract LaTeX formulas from text.
     * Combine all formulas into a single LaTeX document.
     */
    private String extractLatexContent(String text) {
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
} 