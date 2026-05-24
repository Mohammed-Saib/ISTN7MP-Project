package com.example.mpproject.domain.model;

public class PersonalNoteAttachment {

    private String attachmentId;
    private String noteId;
    private String userId;
    private String fileName;
    private String fileType;
    private long fileSizeBytes;
    private String storagePath;
    private String downloadUrl;
    private String uploadStatus;
    private long createdAt;

    public PersonalNoteAttachment(String attachmentId, String noteId, String userId, String fileName, String fileType) {
        this.attachmentId = attachmentId;
        this.noteId = noteId;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadStatus = "UPLOADING";
        this.createdAt = System.currentTimeMillis();
    }

    public String getAttachmentId() { return attachmentId; }
    public String getNoteId() { return noteId; }
    public String getUserId() { return userId; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getStoragePath() { return storagePath; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getUploadStatus() { return uploadStatus; }
    public long getCreatedAt() { return createdAt; }

    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

