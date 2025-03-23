package com.example.image2latex.documentwriter;

import java.util.Date;

/**
 * Class representing a document in the Document Writer feature
 */
public class Document {
    private String id;
    private String title;
    private String content;
    private Date createdDate;
    private Date modifiedDate;
    private String filePath;

    public Document() {
        this.createdDate = new Date();
        this.modifiedDate = new Date();
    }

    public Document(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    public Document(String id, String title, String content, Date createdDate, Date modifiedDate, String filePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.filePath = filePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.modifiedDate = new Date();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
} 