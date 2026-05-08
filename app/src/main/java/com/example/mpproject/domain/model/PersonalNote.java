package com.example.mpproject.domain.model;

// [Domain] Plain Java — no Room or Firebase annotations. A free-form text note; optionally linked to a module.
public class PersonalNote {
    private String noteId;
    private String userId;
    private String moduleId;    // nullable — note doesn't have to belong to a module
    private String title;
    private String content;
    private String shareCode;   // short code used for sharing the note with others
    private boolean isPinned;
    private boolean isShared;
    private long createdAt;
    private long updatedAt;

    public PersonalNote(String noteId, String userId, String title) {
        this.noteId = noteId;
        this.userId = userId;
        this.title = title;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNoteId() { return noteId; }
    public String getUserId() { return userId; }
    public String getModuleId() { return moduleId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getShareCode() { return shareCode; }
    public boolean isPinned() { return isPinned; }
    public boolean isShared() { return isShared; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public void setShared(boolean shared) { isShared = shared; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
