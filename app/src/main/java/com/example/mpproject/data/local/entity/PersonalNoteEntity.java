package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// The user's own typed notes. Freeform — not required to belong to a module.
// Content is stored as Markdown so formatting (bold, italic, headings) is preserved.
@Entity(tableName = "personal_notes",
        foreignKeys = @ForeignKey(
                entity = ModuleEntity.class,
                parentColumns = "moduleId",
                childColumns = "moduleId",
                onDelete = ForeignKey.SET_NULL), // deleting a module sets moduleId to null here (no cascade)
        indices = {
                @Index({"userId", "isPinned"}), // pinned notes first, then by updatedAt
                @Index("moduleId")              // find all notes linked to a specific module
        })
public class PersonalNoteEntity {

    @PrimaryKey
    @NonNull
    private String noteId;     // UUID
    private String userId;     // owner
    private String moduleId;   // optional link to a module; null if not linked or if module was deleted
    private String title;
    private String content;    // Markdown body; empty string for a brand-new note
    private boolean isPinned;  // pinned notes appear at the top of the list
    private boolean isShared;  // true when a shared_notes Firestore document exists for this note
    private String shareCode;  // short alphanumeric code, e.g. 'xK9mP2'; kept for reuse after unsharing
    private long createdAt;    // unix ms
    private long updatedAt;    // unix ms; updated on every auto-save

    public PersonalNoteEntity() {}

    @NonNull public String getNoteId() { return noteId; }
    public void setNoteId(@NonNull String noteId) { this.noteId = noteId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { isShared = shared; }
    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
