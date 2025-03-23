package com.example.image2latex.documentwriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.image2latex.LaTeXPreviewDialog;
import com.example.image2latex.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentWritingActivity extends AppCompatActivity {

    private static final String TAG = "DocumentWritingActivity";
    private EditText documentEditor;
    private TextView latexPreview;
    private Button btnFontSmall, btnFontMedium, btnFontLarge, btnInsertLatex, btnSave;
    private Toolbar toolbar;
    private DocumentManager documentManager;
    private String documentId;
    private Document currentDocument;
    
    private static final int SMALL_TEXT_SIZE = 14;
    private static final int MEDIUM_TEXT_SIZE = 16;
    private static final int LARGE_TEXT_SIZE = 20;
    
    // Regex pattern to identify LaTeX formulas wrapped in $...$ or $$...$$
    private static final Pattern LATEX_PATTERN = Pattern.compile("\\$\\$(.*?)\\$\\$|\\$(.*?)\\$", Pattern.DOTALL);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Setting content view");
            setContentView(R.layout.activity_document_writing);
            
            // Initialize document manager
            Log.d(TAG, "Initializing document manager");
            documentManager = new DocumentManager(this);
            
            // Get document ID from intent
            documentId = getIntent().getStringExtra("document_id");
            if (documentId != null) {
                // Load existing document
                Log.d(TAG, "Loading document with ID: " + documentId);
                currentDocument = documentManager.loadDocument(documentId);
            }
            
            if (currentDocument == null) {
                // Create new document if loading failed
                Log.d(TAG, "Creating new document as current is null");
                currentDocument = new Document();
                currentDocument.setTitle("Untitled Document");
                currentDocument.setContent("");
            }
            
            // Initialize views
            Log.d(TAG, "Initializing views");
            documentEditor = findViewById(R.id.document_editor);
            latexPreview = findViewById(R.id.latex_preview);
            btnFontSmall = findViewById(R.id.btn_font_small);
            btnFontMedium = findViewById(R.id.btn_font_medium);
            btnFontLarge = findViewById(R.id.btn_font_large);
            btnInsertLatex = findViewById(R.id.btn_insert_latex);
            btnSave = findViewById(R.id.btn_save);
            toolbar = findViewById(R.id.toolbar);
            
            // Set up toolbar only if not null
            if (toolbar != null) {
                Log.d(TAG, "Setting up toolbar");
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                Log.w(TAG, "Toolbar is null, skipping toolbar setup");
            }
            
            // Set document content
            Log.d(TAG, "Setting document content");
            documentEditor.setText(currentDocument.getContent());
            
            // Set up button click listeners
            Log.d(TAG, "Setting up button listeners");
            if (btnFontSmall != null) btnFontSmall.setOnClickListener(v -> setFontSize(SMALL_TEXT_SIZE));
            if (btnFontMedium != null) btnFontMedium.setOnClickListener(v -> setFontSize(MEDIUM_TEXT_SIZE));
            if (btnFontLarge != null) btnFontLarge.setOnClickListener(v -> setFontSize(LARGE_TEXT_SIZE));
            if (btnInsertLatex != null) btnInsertLatex.setOnClickListener(v -> showLatexInputDialog());
            if (btnSave != null) btnSave.setOnClickListener(v -> saveDocument());
            
            // Set up text watcher to detect LaTeX formulas
            Log.d(TAG, "Setting up text watcher");
            documentEditor.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    detectLatexFormulas(s.toString());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting document editor: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setFontSize(int sizeSp) {
        documentEditor.setTextSize(sizeSp);
    }
    
    private void detectLatexFormulas(String text) {
        // Find LaTeX patterns in the text
        Matcher matcher = LATEX_PATTERN.matcher(text);
        if (matcher.find()) {
            latexPreview.setVisibility(View.VISIBLE);
            String latexContent = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            showLatexPreview(latexContent);
        } else {
            latexPreview.setVisibility(View.GONE);
        }
    }
    
    private void showLatexPreview(String latexContent) {
        // Display LaTeX preview at the bottom
        latexPreview.setText("LaTeX: " + latexContent);
    }
    
    private void showLatexInputDialog() {
        // Create an input dialog for LaTeX formula
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Insert LaTeX Formula");
        
        final EditText input = new EditText(this);
        input.setHint("Enter your LaTeX formula");
        builder.setView(input);
        
        builder.setPositiveButton("Insert", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String formula = input.getText().toString();
                
                // Insert at cursor position
                int position = documentEditor.getSelectionStart();
                Editable editable = documentEditor.getText();
                
                // Wrap formula in dollar signs
                String wrappedFormula = "$" + formula + "$";
                editable.insert(position, wrappedFormula);
            }
        });
        
        builder.setNeutralButton("Preview", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String formula = input.getText().toString();
                LaTeXPreviewDialog previewDialog = new LaTeXPreviewDialog(DocumentWritingActivity.this, formula);
                previewDialog.show();
            }
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void saveDocument() {
        try {
            String content = documentEditor.getText().toString();
            
            // Update document content
            currentDocument.setContent(content);
            
            // Save the document
            if (documentManager.saveDocument(currentDocument)) {
                Toast.makeText(this, "Document saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save document", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("DocumentWriter", "Error saving document", e);
            Toast.makeText(this, "Error saving document", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 