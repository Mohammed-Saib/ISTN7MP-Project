package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "personal_note_attachments",
        foreignKeys = @ForeignKey(
                entity = PersonalNoteEntity.class,
                parentColumns = "noteId",
                childColumns = "noteId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("noteId"),
                @Index("userId")
        }
)
public class PersonalNoteAttachmentEntity {

    @PrimaryKey
    @NonNull
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

    public PersonalNoteAttachmentEntity() {}

    @NonNull
    public String getAttachmentId() { return attachmentId; }

    public void setAttachmentId(@NonNull String attachmentId) { this.attachmentId = attachmentId; }

    public String getNoteId() { return noteId; }
    public void setNoteId(String noteId) { this.noteId = noteId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

