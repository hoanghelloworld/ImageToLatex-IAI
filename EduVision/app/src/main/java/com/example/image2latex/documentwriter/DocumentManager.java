package com.example.image2latex.documentwriter;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manager class for document operations like save, load, list
 */
public class DocumentManager {
    private static final String TAG = "DocumentManager";
    private static final String DOCUMENTS_DIR = "documents";
    private static final String METADATA_DIR = "document_metadata";
    
    private Context context;
    
    public DocumentManager(Context context) {
        this.context = context;
        ensureDirectoriesExist();
    }
    
    private void ensureDirectoriesExist() {
        File documentsDir = new File(context.getFilesDir(), DOCUMENTS_DIR);
        File metadataDir = new File(context.getFilesDir(), METADATA_DIR);
        
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        
        if (!metadataDir.exists()) {
            metadataDir.mkdirs();
        }
    }
    
    /**
     * Save a document to storage
     */
    public boolean saveDocument(Document document) {
        try {
            // Generate ID and filename if not set
            if (document.getId() == null) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                document.setId(timestamp);
            }
            
            String fileName = document.getId() + ".txt";
            
            // Save document content
            File documentsDir = new File(context.getFilesDir(), DOCUMENTS_DIR);
            File documentFile = new File(documentsDir, fileName);
            
            FileOutputStream fos = new FileOutputStream(documentFile);
            fos.write(document.getContent().getBytes());
            fos.close();
            
            // Set file path
            document.setFilePath(documentFile.getAbsolutePath());
            
            // Save metadata
            saveMetadata(document);
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving document", e);
            return false;
        }
    }
    
    /**
     * Load a document from storage by ID
     */
    public Document loadDocument(String documentId) {
        try {
            String fileName = documentId + ".txt";
            
            // Load document content
            File documentsDir = new File(context.getFilesDir(), DOCUMENTS_DIR);
            File documentFile = new File(documentsDir, fileName);
            
            if (!documentFile.exists()) {
                return null;
            }
            
            FileInputStream fis = new FileInputStream(documentFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            
            reader.close();
            fis.close();
            
            // Load metadata
            Document document = loadMetadata(documentId);
            
            if (document == null) {
                // If metadata is missing, create basic document
                document = new Document();
                document.setId(documentId);
                document.setTitle("Untitled Document");
                document.setFilePath(documentFile.getAbsolutePath());
            }
            
            document.setContent(contentBuilder.toString());
            
            return document;
        } catch (IOException e) {
            Log.e(TAG, "Error loading document", e);
            return null;
        }
    }
    
    /**
     * Get list of all documents
     */
    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        
        try {
            File metadataDir = new File(context.getFilesDir(), METADATA_DIR);
            File[] metadataFiles = metadataDir.listFiles();
            
            if (metadataFiles != null) {
                for (File file : metadataFiles) {
                    String documentId = file.getName().replace(".meta", "");
                    Document document = loadMetadata(documentId);
                    if (document != null) {
                        documents.add(document);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading document list", e);
        }
        
        return documents;
    }
    
    /**
     * Delete a document
     */
    public boolean deleteDocument(String documentId) {
        try {
            String fileName = documentId + ".txt";
            String metaFileName = documentId + ".meta";
            
            File documentsDir = new File(context.getFilesDir(), DOCUMENTS_DIR);
            File metadataDir = new File(context.getFilesDir(), METADATA_DIR);
            
            File documentFile = new File(documentsDir, fileName);
            File metadataFile = new File(metadataDir, metaFileName);
            
            boolean documentDeleted = true;
            boolean metadataDeleted = true;
            
            if (documentFile.exists()) {
                documentDeleted = documentFile.delete();
            }
            
            if (metadataFile.exists()) {
                metadataDeleted = metadataFile.delete();
            }
            
            return documentDeleted && metadataDeleted;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting document", e);
            return false;
        }
    }
    
    /**
     * Save document metadata
     */
    private void saveMetadata(Document document) throws IOException {
        File metadataDir = new File(context.getFilesDir(), METADATA_DIR);
        File metadataFile = new File(metadataDir, document.getId() + ".meta");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append("id=").append(document.getId()).append("\n");
        metadataBuilder.append("title=").append(document.getTitle()).append("\n");
        metadataBuilder.append("created=").append(dateFormat.format(document.getCreatedDate())).append("\n");
        metadataBuilder.append("modified=").append(dateFormat.format(document.getModifiedDate())).append("\n");
        metadataBuilder.append("path=").append(document.getFilePath()).append("\n");
        
        FileOutputStream fos = new FileOutputStream(metadataFile);
        fos.write(metadataBuilder.toString().getBytes());
        fos.close();
    }
    
    /**
     * Load document metadata
     */
    private Document loadMetadata(String documentId) {
        try {
            File metadataDir = new File(context.getFilesDir(), METADATA_DIR);
            File metadataFile = new File(metadataDir, documentId + ".meta");
            
            if (!metadataFile.exists()) {
                return null;
            }
            
            FileInputStream fis = new FileInputStream(metadataFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            
            Document document = new Document();
            String line;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                
                String key = parts[0];
                String value = parts[1];
                
                switch (key) {
                    case "id":
                        document.setId(value);
                        break;
                    case "title":
                        document.setTitle(value);
                        break;
                    case "created":
                        try {
                            document.setCreatedDate(dateFormat.parse(value));
                        } catch (ParseException e) {
                            document.setCreatedDate(new Date());
                        }
                        break;
                    case "modified":
                        try {
                            document.setModifiedDate(dateFormat.parse(value));
                        } catch (ParseException e) {
                            document.setModifiedDate(new Date());
                        }
                        break;
                    case "path":
                        document.setFilePath(value);
                        break;
                }
            }
            
            reader.close();
            fis.close();
            
            return document;
        } catch (IOException e) {
            Log.e(TAG, "Error loading document metadata", e);
            return null;
        }
    }
} 