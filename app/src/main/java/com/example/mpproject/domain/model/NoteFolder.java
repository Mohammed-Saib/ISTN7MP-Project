package com.example.mpproject.domain.model;

public class NoteFolder {

    private String folderId;
    private String userId;
    private String name;
    private long createdAt;
    private long updatedAt;

    public NoteFolder(String folderId, String userId, String name) {
        this.folderId = folderId;
        this.userId = userId;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFolderId() {
        return folderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}