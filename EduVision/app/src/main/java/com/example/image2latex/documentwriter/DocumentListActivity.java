package com.example.image2latex.documentwriter;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.image2latex.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocumentListActivity extends AppCompatActivity implements DocumentAdapter.DocumentClickListener {

    private static final String TAG = "DocumentListActivity";
    private RecyclerView documentList;
    private TextView emptyView;
    private FloatingActionButton fabAddDocument;
    private DocumentAdapter adapter;
    private DocumentManager documentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Setting content view");
            setContentView(R.layout.activity_document_list);

            // Initialize views
            Log.d(TAG, "Initializing views");
            documentList = findViewById(R.id.document_list);
            emptyView = findViewById(R.id.empty_view);
            fabAddDocument = findViewById(R.id.fab_add_document);
            Toolbar toolbar = findViewById(R.id.toolbar);

            // Set up toolbar - only if not null
            if (toolbar != null) {
                Log.d(TAG, "Setting up toolbar");
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                Log.w(TAG, "Toolbar is null, skipping toolbar setup");
            }

            // Initialize document manager
            Log.d(TAG, "Initializing document manager");
            documentManager = new DocumentManager(this);

            // Set up RecyclerView
            Log.d(TAG, "Setting up RecyclerView");
            documentList.setLayoutManager(new LinearLayoutManager(this));
            adapter = new DocumentAdapter(this);
            documentList.setAdapter(adapter);

            // Set up FAB
            Log.d(TAG, "Setting up FAB");
            fabAddDocument.setOnClickListener(v -> createNewDocument());

            // Load documents
            Log.d(TAG, "Loading documents");
            loadDocuments();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting document list: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Reload documents when returning to the activity
            loadDocuments();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    private void loadDocuments() {
        try {
            List<Document> documents;
            try {
                documents = documentManager.getAllDocuments();
            } catch (Exception e) {
                Log.e(TAG, "Error loading documents from manager", e);
                documents = new ArrayList<>();
            }
            
            adapter.setDocuments(documents);

            // Show empty view if no documents
            if (documents.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                documentList.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                documentList.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in loadDocuments", e);
        }
    }

    private void createNewDocument() {
        // Create a new empty document
        Document document = new Document();
        document.setTitle("Untitled Document");
        document.setContent("");

        // Save the document to get an ID
        if (documentManager.saveDocument(document)) {
            // Open document editor
            Intent intent = new Intent(this, DocumentWritingActivity.class);
            intent.putExtra("document_id", document.getId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Failed to create new document", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDocumentClick(Document document) {
        // Open document in editor
        Intent intent = new Intent(this, DocumentWritingActivity.class);
        intent.putExtra("document_id", document.getId());
        startActivity(intent);
    }

    @Override
    public void onDocumentMenuClick(View view, Document document) {
        // Show popup menu with options
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.document_options_menu);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_rename) {
                // Rename document
                showRenameDialog(document);
                return true;
            } else if (itemId == R.id.action_delete) {
                // Delete document
                deleteDocument(document);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showRenameDialog(Document document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Document");
        
        // Create input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(document.getTitle());
        input.selectAll(); // Select all text for easy editing
        
        // Set padding for better appearance
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);
        
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                if (documentManager.renameDocument(document.getId(), newTitle)) {
                    // Update the document object
                    document.setTitle(newTitle);
                    document.setModifiedDate(new Date());
                    
                    // Refresh the list
                    loadDocuments();
                    
                    Toast.makeText(this, "Document renamed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to rename document", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Focus on input and show keyboard
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void deleteDocument(Document document) {
        // Delete the document
        if (documentManager.deleteDocument(document.getId())) {
            // Refresh the list
            loadDocuments();
            Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete document", Toast.LENGTH_SHORT).show();
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