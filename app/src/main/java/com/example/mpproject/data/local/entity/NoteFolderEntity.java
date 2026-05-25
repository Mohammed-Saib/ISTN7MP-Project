package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "note_folders",
        indices = {
                @Index("userId")
        }
)
public class NoteFolderEntity {

    @PrimaryKey
    @NonNull
    private String folderId;

    private String userId;
    private String name;
    private long createdAt;
    private long updatedAt;

    public NoteFolderEntity() {}

    @NonNull
    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(@NonNull String folderId) {
        this.folderId = folderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}