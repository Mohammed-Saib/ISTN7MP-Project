package com.example.mpproject.domain.model;

// [Domain] Plain Java — no Room or Firebase annotations. Represents a file uploaded to a module (PDF, image, etc.)
// The actual file lives in Firebase Storage; only the URI is stored locally.
public class ModuleNote {
    private String noteId;
    private String moduleId;
    private String userId;
    private String title;
    private String fileName;
    private String fileType;     // e.g. "application/pdf"
    private String storageUri;   // Firebase Storage download URL
    private String uploadStatus; // "pending", "uploading", "done", "failed"
    private long fileSizeBytes;
    private long createdAt;

    public ModuleNote(String noteId, String moduleId, String userId, String title,
                      String fileName, String fileType) {
        this.noteId = noteId;
        this.moduleId = moduleId;
        this.userId = userId;
        this.title = title;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadStatus = "pending";
        this.createdAt = System.currentTimeMillis();
    }

    public String getNoteId() { return noteId; }
    public String getModuleId() { return moduleId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getStorageUri() { return storageUri; }
    public String getUploadStatus() { return uploadStatus; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public long getCreatedAt() { return createdAt; }

    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
}
