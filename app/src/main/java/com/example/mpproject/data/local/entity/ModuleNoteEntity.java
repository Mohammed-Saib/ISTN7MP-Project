package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Metadata for a file (PDF, image) uploaded to a module.
// The file itself lives in Firebase Storage — only the download URI is stored here.
@Entity(tableName = "module_notes",
        foreignKeys = @ForeignKey(
                entity = ModuleEntity.class,
                parentColumns = "moduleId",
                childColumns = "moduleId",
                onDelete = ForeignKey.CASCADE), // deleting a module cascades to delete its files
        indices = {
                @Index("moduleId"), // fast lookup of all files for a given module
                @Index("userId")
        })
public class ModuleNoteEntity {

    @PrimaryKey
    @NonNull
    private String noteId;         // UUID
    private String moduleId;       // FK → modules.moduleId (CASCADE delete)
    private String userId;         // denormalised so we can query by user without a join
    private String title;          // display name, e.g. 'Week 3 Lecture Slides'
    private String fileName;       // original filename with extension, e.g. 'week3.pdf'
    private String fileType;       // PDF | IMAGE | OTHER
    private long fileSizeBytes;    // shown as a size label in the UI
    private String storageUri;     // Firebase Storage download URI; set after upload completes
    private String uploadStatus;   // UPLOADING | DONE | FAILED
    private long createdAt;        // unix ms

    public ModuleNoteEntity() {}

    @NonNull public String getNoteId() { return noteId; }
    public void setNoteId(@NonNull String noteId) { this.noteId = noteId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getStorageUri() { return storageUri; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
