package com.example.image2latex.documentwriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.image2latex.LaTeXPreviewDialog;
import com.example.image2latex.R;
import com.example.image2latex.chatbot.ChatbotHelper;
import com.example.image2latex.chatbot.ChatbotHelperSingleton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentWritingActivity extends AppCompatActivity {

    private static final String TAG = "DocumentWritingActivity";
    private EditText documentEditor;
    private TextView latexPreview;
    private Button btnFontSmall, btnFontMedium, btnFontLarge, btnInsertLatex, btnSave, btnRender;
    private Toolbar toolbar;
    private DocumentManager documentManager;
    private String documentId;
    private Document currentDocument;

    // Text formatting toolbar
    private LinearLayout formattingToolbar;
    private Button btnBold, btnItalic, btnUnderline, btnTextColor, btnTurnInto, btnAskAI;

    private static final int SMALL_TEXT_SIZE = 14;
    private static final int MEDIUM_TEXT_SIZE = 16;
    private static final int LARGE_TEXT_SIZE = 20;

    // AI request types
    private static final String AI_IMPROVE_WRITING = "Improve my writing: ";
    private static final String AI_FIX_GRAMMAR = "Fix spelling and grammar: ";
    private static final String AI_TRANSLATE_PREFIX = "Translate to ";
    private static final String AI_EXPLAIN = "Explain this: ";
    private static final String AI_MAKE_SHORTER = "Make this shorter: ";
    private static final String AI_MAKE_LONGER = "Make this longer with more details: ";
    private static final String AI_CHANGE_TONE = "Change the tone to be more professional: ";

    // Language codes for translation
    private static final String[] LANGUAGE_NAMES = {
        "English", "Vietnamese", "Chinese", "Japanese", "French", "Korean", "Spanish", "German"
    };

    // Regex pattern to identify LaTeX formulas wrapped in $...$ or $$...$$
    private static final Pattern LATEX_PATTERN = Pattern.compile("\\$\\$(.*?)\\$\\$|\\$(.*?)\\$", Pattern.DOTALL);

    private ChatbotHelper aiHelper;
    private boolean processingAiRequest = false;

    private static final int PREVIEW_HTML = 1;
    private static final int EXPORT_HTML = 2;

    private View previewFrame;
    private TextView previewTitle;
    private TextView previewContent;
    private Button previewInsertButton;
    private Button previewCloseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Setting content view");
            try {
                // Try to inflate the primary layout with MaterialComponents
                setContentView(R.layout.activity_document_writing);
            } catch (Exception e) {
                // If that fails, use the fallback layout with regular buttons
                Log.w(TAG, "Failed to load primary layout, using fallback", e);
                setContentView(R.layout.activity_document_writing_fallback);
            }

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
            btnRender = findViewById(R.id.btn_render);
            toolbar = findViewById(R.id.toolbar);

            // Initialize formatting toolbar
            formattingToolbar = findViewById(R.id.formatting_toolbar);
            btnBold = findViewById(R.id.btn_bold);
            btnItalic = findViewById(R.id.btn_italic);
            btnUnderline = findViewById(R.id.btn_underline);
            btnTextColor = findViewById(R.id.btn_text_color);
            btnTurnInto = findViewById(R.id.btn_turn_into);
            btnAskAI = findViewById(R.id.btn_ask_ai);

            // Initialize preview frame
            previewFrame = findViewById(R.id.ai_preview_frame);
            previewTitle = findViewById(R.id.ai_preview_title);
            previewContent = findViewById(R.id.ai_preview_content);
            previewInsertButton = findViewById(R.id.ai_preview_insert);
            previewCloseButton = findViewById(R.id.ai_preview_close);

            if (previewCloseButton != null) {
                previewCloseButton.setOnClickListener(v -> {
                    if (previewFrame != null) {
                        previewFrame.setVisibility(View.GONE);
                    }
                });
            }

            if (previewInsertButton != null) {
                previewInsertButton.setOnClickListener(v -> {
                    if (previewFrame != null && previewContent != null) {
                        // Insert content at cursor position
                        int cursorPosition = documentEditor.getSelectionStart();
                        Editable editable = documentEditor.getText();
                        editable.insert(cursorPosition, previewContent.getText());
                        previewFrame.setVisibility(View.GONE);
                    }
                });
            }

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
            // Set up formatting button listeners
            if (btnBold != null) btnBold.setOnClickListener(v -> applyBoldStyle());
            if (btnItalic != null) btnItalic.setOnClickListener(v -> applyItalicStyle());
            if (btnUnderline != null) btnUnderline.setOnClickListener(v -> applyUnderlineStyle());
            if (btnTextColor != null) btnTextColor.setOnClickListener(v -> showTextColorMenu());
            if (btnTurnInto != null) btnTurnInto.setOnClickListener(v -> showTurnIntoMenu());
            if (btnAskAI != null) btnAskAI.setOnClickListener(v -> showAskAIMenu());

            // Initialize AI helper
            aiHelper = ChatbotHelperSingleton.getInstance(this);

            // Set up text selection listener
            documentEditor.setOnClickListener(v -> checkTextSelection());

            // Add this code to override default text selection actions
            documentEditor.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // Clear the default menu items to prevent the Android search icon from appearing
                    menu.clear();
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // Keep the menu clear - this prevents Android from adding search options
                    menu.clear();
                    // Show our custom formatting toolbar instead
                    checkTextSelection();
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    // Hide formatting toolbar when selection action mode is destroyed
                    formattingToolbar.setVisibility(View.GONE);
                }
            });
            if (btnRender != null) btnRender.setOnClickListener(v -> renderDocument());
            // Set up text watcher to detect LaTeX formulas
            Log.d(TAG, "Setting up text watcher");
            documentEditor.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    checkTextSelection();
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

    private void checkTextSelection() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            // Text is selected, show formatting toolbar
            formattingToolbar.setVisibility(View.VISIBLE);
        } else {
            // No text selected, hide formatting toolbar
            formattingToolbar.setVisibility(View.GONE);
        }
    }

    private void applyBoldStyle() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();

            // Check for existing bold spans
            StyleSpan[] spans = editable.getSpans(selStart, selEnd, StyleSpan.class);
            boolean hasBold = false;

            for (StyleSpan span : spans) {
                if (span.getStyle() == Typeface.BOLD) {
                    editable.removeSpan(span);
                    hasBold = true;
                }
            }

            if (!hasBold) {
                // Apply bold with correct vertical positioning
                StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                editable.setSpan(boldSpan, selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Force redraw to ensure correct rendering but preserve cursor position
            int cursorPosition = documentEditor.getSelectionEnd();
            documentEditor.setText(editable);
            documentEditor.setSelection(cursorPosition);
            
            // Reset the action mode to prevent Android from showing search
            documentEditor.clearFocus();
            documentEditor.requestFocus();
        }
    }

    private void applyItalicStyle() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();

            // Check for existing italic spans
            StyleSpan[] spans = editable.getSpans(selStart, selEnd, StyleSpan.class);
            boolean hasItalic = false;

            for (StyleSpan span : spans) {
                if (span.getStyle() == Typeface.ITALIC) {
                    editable.removeSpan(span);
                    hasItalic = true;
                }
            }

            if (!hasItalic) {
                // Apply italic with correct vertical positioning
                StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);
                editable.setSpan(italicSpan, selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Force redraw to ensure correct rendering but preserve cursor position
            int cursorPosition = documentEditor.getSelectionEnd();
            documentEditor.setText(editable);
            documentEditor.setSelection(cursorPosition);
            
            // Reset the action mode to prevent Android from showing search
            documentEditor.clearFocus();
            documentEditor.requestFocus();
        }
    }

    private void applyUnderlineStyle() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();

            // Check for existing underline spans
            UnderlineSpan[] spans = editable.getSpans(selStart, selEnd, UnderlineSpan.class);
            boolean hasUnderline = false;

            for (UnderlineSpan span : spans) {
                editable.removeSpan(span);
                hasUnderline = true;
            }

            if (!hasUnderline) {
                // Apply underline with correct vertical positioning
                UnderlineSpan underlineSpan = new UnderlineSpan();
                editable.setSpan(underlineSpan, selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Force redraw to ensure correct rendering but preserve cursor position
            int cursorPosition = documentEditor.getSelectionEnd();
            documentEditor.setText(editable);
            documentEditor.setSelection(cursorPosition);
            
            // Reset the action mode to prevent Android from showing search
            documentEditor.clearFocus();
            documentEditor.requestFocus();
        }
    }

    private void showTextColorMenu() {
        PopupMenu popup = new PopupMenu(this, btnTextColor);
        popup.getMenu().add("Black").setOnMenuItemClickListener(item -> {
            applyTextColor(Color.BLACK);
            return true;
        });
        popup.getMenu().add("Red").setOnMenuItemClickListener(item -> {
            applyTextColor(Color.RED);
            return true;
        });
        popup.getMenu().add("Blue").setOnMenuItemClickListener(item -> {
            applyTextColor(Color.BLUE);
            return true;
        });
        popup.getMenu().add("Green").setOnMenuItemClickListener(item -> {
            applyTextColor(Color.GREEN);
            return true;
        });
        popup.getMenu().add("Yellow Background").setOnMenuItemClickListener(item -> {
            applyBackgroundColor(Color.YELLOW);
            return true;
        });
        popup.show();
    }

    private void applyTextColor(int color) {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();

            // Remove existing color spans
            ForegroundColorSpan[] spans = editable.getSpans(selStart, selEnd, ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                editable.removeSpan(span);
            }

            // Apply new color
            editable.setSpan(new ForegroundColorSpan(color), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyBackgroundColor(int color) {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();

            // Remove existing background color spans
            BackgroundColorSpan[] spans = editable.getSpans(selStart, selEnd, BackgroundColorSpan.class);
            for (BackgroundColorSpan span : spans) {
                editable.removeSpan(span);
            }

            // Apply new background color
            editable.setSpan(new BackgroundColorSpan(color), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void showTurnIntoMenu() {
        PopupMenu popup = new PopupMenu(this, btnTurnInto);
        popup.getMenu().add("Plain Text").setOnMenuItemClickListener(item -> {
            turnIntoPlainText();
            return true;
        });
        popup.getMenu().add("Heading 1").setOnMenuItemClickListener(item -> {
            turnIntoHeading(1.5f);
            return true;
        });
        popup.getMenu().add("Heading 2").setOnMenuItemClickListener(item -> {
            turnIntoHeading(1.3f);
            return true;
        });
        popup.getMenu().add("Heading 3").setOnMenuItemClickListener(item -> {
            turnIntoHeading(1.1f);
            return true;
        });
        popup.getMenu().add("Code").setOnMenuItemClickListener(item -> {
            turnIntoCode();
            return true;
        });
        popup.getMenu().add("Block Quote").setOnMenuItemClickListener(item -> {
            turnIntoBlockQuote();
            return true;
        });
        popup.getMenu().add("Bulleted List").setOnMenuItemClickListener(item -> {
            turnIntoBulletedList();
            return true;
        });
        popup.show();
    }

    private void turnIntoPlainText() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();
            Object[] spans = editable.getSpans(selStart, selEnd, Object.class);

            for (Object span : spans) {
                editable.removeSpan(span);
            }
        }
    }

    private void turnIntoHeading(float sizeMultiplier) {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();

            // Remove existing size spans
            RelativeSizeSpan[] spans = editable.getSpans(selStart, selEnd, RelativeSizeSpan.class);
            for (RelativeSizeSpan span : spans) {
                editable.removeSpan(span);
            }

            // Apply heading style
            editable.setSpan(new RelativeSizeSpan(sizeMultiplier), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editable.setSpan(new StyleSpan(Typeface.BOLD), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void turnIntoCode() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();
            String selectedText = editable.subSequence(selStart, selEnd).toString();

            // Format as code (with monospace font and background)
            editable.replace(selStart, selEnd, "`" + selectedText + "`");

            // Apply code styling
            editable.setSpan(new BackgroundColorSpan(Color.parseColor("#f0f0f0")),
                    selStart, selStart + selectedText.length() + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void turnIntoBlockQuote() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();
            String selectedText = editable.subSequence(selStart, selEnd).toString();

            // Format as block quote
            String quotedText = "> " + selectedText.replace("\n", "\n> ");
            editable.replace(selStart, selEnd, quotedText);

            // Apply quote styling
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#666666")),
                    selStart, selStart + quotedText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editable.setSpan(new StyleSpan(Typeface.ITALIC),
                    selStart, selStart + quotedText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void turnIntoBulletedList() {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();
            String selectedText = editable.subSequence(selStart, selEnd).toString();

            // Split text by lines
            String[] lines = selectedText.split("\n");
            StringBuilder listText = new StringBuilder();

            for (String line : lines) {
                if (!TextUtils.isEmpty(line.trim())) {
                    listText.append("• ").append(line).append("\n");
                } else {
                    listText.append("\n");
                }
            }

            // Replace with bulleted list
            editable.replace(selStart, selEnd, listText.toString());
        }
    }

    private void showAskAIMenu() {
        if (processingAiRequest) {
            Toast.makeText(this, "Already processing an AI request, please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popup = new PopupMenu(this, btnAskAI);
        popup.getMenu().add("Ask AI Anything").setOnMenuItemClickListener(item -> {
            showAskAIDialog();
            return true;
        });
        popup.getMenu().add("Explain").setOnMenuItemClickListener(item -> {
            sendAIRequest(AI_EXPLAIN);
            return true;
        });
        popup.getMenu().add("Improve Writing").setOnMenuItemClickListener(item -> {
            sendAIRequest(AI_IMPROVE_WRITING);
            return true;
        });
        popup.getMenu().add("Fix Spelling & Grammar").setOnMenuItemClickListener(item -> {
            sendAIRequest(AI_FIX_GRAMMAR);
            return true;
        });
        
        // Add a submenu for Translation with multiple language options
        MenuItem translateItem = popup.getMenu().add("Translate");
        translateItem.setOnMenuItemClickListener(item -> {
            showTranslationLanguageMenu();
            return true;
        });
        
        popup.getMenu().add("Make Shorter").setOnMenuItemClickListener(item -> {
            sendAIRequest(AI_MAKE_SHORTER);
            return true;
        });
        popup.getMenu().add("Make Longer").setOnMenuItemClickListener(item -> {
            sendAIRequest(AI_MAKE_LONGER);
            return true;
        });
        popup.getMenu().add("Change Tone").setOnMenuItemClickListener(item -> {
            sendAIRequest(AI_CHANGE_TONE);
            return true;
        });
        popup.show();
    }

    private void showTranslationLanguageMenu() {
        PopupMenu languageMenu = new PopupMenu(this, btnAskAI);
        
        for (String language : LANGUAGE_NAMES) {
            languageMenu.getMenu().add(language).setOnMenuItemClickListener(item -> {
                sendAIRequest(AI_TRANSLATE_PREFIX + language + ": ");
                return true;
            });
        }
        
        languageMenu.show();
    }

    private void showAskAIDialog() {
        // Get any currently selected text from the editor
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();
        final String selectedText = selStart != selEnd ? 
            documentEditor.getText().subSequence(selStart, selEnd).toString() : "";
        
        // Create and configure the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ask AI Anything");
        
        // Create a layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10);
        
        // Create the input field
        final EditText input = new EditText(this);
        input.setHint("What would you like to ask?");
        
        // Add info about selected text if any
        TextView selectionInfo = new TextView(this);
        
        if (!selectedText.isEmpty()) {
            String truncatedText = selectedText.length() > 50 
                ? selectedText.substring(0, 47) + "..." 
                : selectedText;
            selectionInfo.setText("Your query will include the selected text: \"" + truncatedText + "\"");
            selectionInfo.setTextColor(Color.GRAY);
        } else {
            selectionInfo.setText("No text is currently selected.");
            selectionInfo.setTextColor(Color.GRAY);
        }
        
        // Add views to layout
        layout.addView(input);
        layout.addView(selectionInfo);
        
        builder.setView(layout);
        
        // Set up buttons
        builder.setPositiveButton("Ask", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String question = input.getText().toString().trim();
                if (!TextUtils.isEmpty(question)) {
                    // Include the selected text in the query if available
                    if (!TextUtils.isEmpty(selectedText)) {
                        processAIQueryWithSelection(question, selectedText);
                    } else {
                        processCustomAIRequest(question);
                    }
                }
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
    
    private void processAIQueryWithSelection(String question, String selectedText) {
        processingAiRequest = true;
        Toast.makeText(this, "Processing request...", Toast.LENGTH_SHORT).show();
        
        // Show loading indicator
        documentEditor.setEnabled(false);
        
        // Format the query to include both the question and the selected text
        String formattedQuery = question + "\n\nReference Text: " + selectedText;
        
        aiHelper.sendMessage(formattedQuery, new ChatbotHelper.ChatResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    // Show the response in the preview frame
                    if (previewFrame != null && previewTitle != null && previewContent != null) {
                        previewTitle.setText("AI Response");
                        previewContent.setText(response);
                        previewFrame.setVisibility(View.VISIBLE);
                        
                        // Make insert button visible
                        if (previewInsertButton != null) {
                            previewInsertButton.setVisibility(View.VISIBLE);
                        }
                    }
                    
                    documentEditor.setEnabled(true);
                    processingAiRequest = false;
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DocumentWritingActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    documentEditor.setEnabled(true);
                    processingAiRequest = false;
                });
            }
        });
    }

    private void processCustomAIRequest(String question) {
        processingAiRequest = true;
        Toast.makeText(this, "Processing request...", Toast.LENGTH_SHORT).show();

        // Show loading indicator
        documentEditor.setEnabled(false);

        // Clear any previous response in the preview frame
        if (previewContent != null) {
            previewContent.setText("");
        }

        aiHelper.sendMessage(question, new ChatbotHelper.ChatResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    // Show the response in the preview frame
                    if (previewFrame != null && previewTitle != null && previewContent != null) {
                        previewTitle.setText("AI Response");
                        previewContent.setText(response);
                        previewFrame.setVisibility(View.VISIBLE);

                        // Make insert button visible
                        if (previewInsertButton != null) {
                            previewInsertButton.setVisibility(View.VISIBLE);
                        }
                    }

                    documentEditor.setEnabled(true);
                    processingAiRequest = false;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DocumentWritingActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    documentEditor.setEnabled(true);
                    processingAiRequest = false;
                });
            }
        });
    }

    private void sendAIRequest(String requestType) {
        int selStart = documentEditor.getSelectionStart();
        int selEnd = documentEditor.getSelectionEnd();

        if (selStart != selEnd) {
            Editable editable = documentEditor.getText();
            
            // Important: Get only the currently selected text, not previous selections
            String selectedText = editable.subSequence(selStart, selEnd).toString();

            if (!TextUtils.isEmpty(selectedText)) {
                processingAiRequest = true;
                Toast.makeText(this, "Processing request...", Toast.LENGTH_SHORT).show();

                // Show loading indicator
                documentEditor.setEnabled(false);

                // Clear any previous response in the preview frame
                if (previewContent != null) {
                    previewContent.setText("");
                }
                
                // Extract source language for better translation if it's a translation request
                String prompt = requestType + selectedText;
                if (requestType.startsWith(AI_TRANSLATE_PREFIX)) {
                    // For translation, try to detect what language they're starting with
                    prompt += "\n\nDetect the source language and translate to the requested language.";
                }

                // Send only the current selection to the AI
                aiHelper.sendMessage(prompt, new ChatbotHelper.ChatResponseListener() {
                    @Override
                    public void onResponse(String response) {
                        runOnUiThread(() -> {
                            // Show the response in the preview frame
                            if (previewFrame != null && previewTitle != null && previewContent != null) {
                                // Set an appropriate title based on the request type
                                String title = "AI Response";
                                
                                if (requestType.startsWith(AI_TRANSLATE_PREFIX)) {
                                    title = "Translation";
                                } else if (requestType.equals(AI_EXPLAIN)) {
                                    title = "Explanation";
                                } else if (requestType.equals(AI_IMPROVE_WRITING)) {
                                    title = "Improved Writing";
                                } else if (requestType.equals(AI_FIX_GRAMMAR)) {
                                    title = "Grammar Correction";
                                }
                                
                                previewTitle.setText(title);
                                previewContent.setText(response);
                                previewFrame.setVisibility(View.VISIBLE);

                                // Make insert button visible
                                if (previewInsertButton != null) {
                                    previewInsertButton.setVisibility(View.VISIBLE);
                                }
                            }

                            documentEditor.setEnabled(true);
                            processingAiRequest = false;
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(DocumentWritingActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            documentEditor.setEnabled(true);
                            processingAiRequest = false;
                        });
                    }
                });
            }
        } else {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFontSize(int sizeSp) {
        documentEditor.setTextSize(sizeSp);
    }

    private void detectLatexFormulas(String text) {
        // Find LaTeX patterns in the text
        Matcher matcher = LATEX_PATTERN.matcher(text);
        if (matcher.find()) {
            latexPreview.setVisibility(View.GONE);
        } else {
            latexPreview.setVisibility(View.GONE);
        }
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

    
    private void renderDocument() {
        try {
            // Save the document first
            saveDocument();
            
            // Get the content
            String content = documentEditor.getText().toString();
            
            // Start the HTML renderer activity
            Intent intent = new Intent(this, HtmlRendererActivity.class);
            intent.putExtra("document_content", content);
            intent.putExtra("document_id", currentDocument.getId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error rendering document", e);
            Toast.makeText(this, "Error rendering document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    public String renderHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; padding: 20px; }");
        html.append("h1 { font-size: 24px; color: #333; }");
        html.append("h2 { font-size: 20px; color: #444; }");
        html.append("h3 { font-size: 18px; color: #555; }");
        html.append("code { background-color: #f5f5f5; padding: 2px 4px; border-radius: 3px; font-family: monospace; }");
        html.append("blockquote { border-left: 4px solid #ccc; margin-left: 0; padding-left: 16px; color: #666; font-style: italic; }");
        html.append("ul { padding-left: 20px; }");
        html.append(".katex-display { overflow-x: auto; padding: 8px 0; }");
        html.append(".katex { font-size: 1.1em; }");
        html.append(".math { color: #0000CC; }");
        html.append("</style>");
        html.append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css'>");
        html.append("<script src='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js'></script>");
        html.append("<script src='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js'></script>");
        html.append("</head><body>");

        // Add document title
        html.append("<h1>").append(currentDocument.getTitle()).append("</h1>");

        // Process content with spans - get a fresh copy to avoid conflicting with previous formatting
        Editable content = documentEditor.getText();
        String plainText = content.toString();

        // First, we'll process spans for formatting
        // We'll mark each formatted portion with a unique identifier
        SpannableStringBuilder processedText = new SpannableStringBuilder(plainText);
        
        // Process bold spans
        StyleSpan[] styleSpans = content.getSpans(0, content.length(), StyleSpan.class);
        for (StyleSpan span : styleSpans) {
            int start = content.getSpanStart(span);
            int end = content.getSpanEnd(span);
            
            if (start >= 0 && end > start && end <= processedText.length()) {
                if (span.getStyle() == Typeface.BOLD) {
                    processedText.replace(start, end, 
                        "<strong>" + processedText.subSequence(start, end) + "</strong>");
                } else if (span.getStyle() == Typeface.ITALIC) {
                    processedText.replace(start, end, 
                        "<em>" + processedText.subSequence(start, end) + "</em>");
                }
            }
        }

        // Process underline spans
        UnderlineSpan[] underlineSpans = content.getSpans(0, content.length(), UnderlineSpan.class);
        for (UnderlineSpan span : underlineSpans) {
            int start = content.getSpanStart(span);
            int end = content.getSpanEnd(span);
            
            if (start >= 0 && end > start && end <= processedText.length()) {
                processedText.replace(start, end, 
                    "<u>" + processedText.subSequence(start, end) + "</u>");
            }
        }

        String htmlContent = processedText.toString();
        
        // Process LaTeX with regex
        Matcher matcher = LATEX_PATTERN.matcher(htmlContent);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String formula = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String replacement = "<span class='math'>$" + formula + "$</span>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        htmlContent = sb.toString();

        // Add processed content to HTML
        html.append(htmlContent);

        // Add closing script to render LaTeX
        html.append("<script>");
        html.append("document.addEventListener('DOMContentLoaded', function() {");
        html.append("  renderMathInElement(document.body, {");
        html.append("    delimiters: [{left: '$$', right: '$$', display: true}, {left: '$', right: '$', display: false}],");
        html.append("    throwOnError: false,");
        html.append("    output: 'html'"); // Use HTML output for better display
        html.append("  });");
        html.append("});");
        html.append("</script>");
        html.append("</body></html>");

        return html.toString();
    }

    private void showHtmlPreview(int mode) {
        String html = renderHtml();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("HTML Preview");

        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        
        // Add JavaScript interface for better debugging if needed
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void showToast(String message) {
                runOnUiThread(() -> Toast.makeText(DocumentWritingActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        }, "Android");
        
        webView.loadDataWithBaseURL("https://cdn.jsdelivr.net/", html, "text/html", "UTF-8", null);

        // Set a proper layout for the WebView with the properly imported ViewGroup class
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        builder.setView(webView);
        builder.setPositiveButton("Close", null);

        if (mode == EXPORT_HTML) {
            builder.setNeutralButton("Save as HTML", (dialog, which) -> {
                saveHtmlToFile(html);
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void exportHtml() {
        showHtmlPreview(EXPORT_HTML);
    }

    private void saveHtmlToFile(String html) {
        try {
            String fileName = currentDocument.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".html";
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File htmlFile = new File(downloadsDir, fileName);
            FileOutputStream fos = new FileOutputStream(htmlFile);
            fos.write(html.getBytes());
            fos.close();

            Toast.makeText(this, "Saved to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving HTML: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error saving HTML", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_preview_html) {
            // Show HTML preview
            showHtmlPreview(PREVIEW_HTML);
            return true;
        } else if (id == R.id.action_export_html) {
            // Export as HTML
            exportHtml();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}