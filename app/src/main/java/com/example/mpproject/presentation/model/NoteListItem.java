package com.example.mpproject.presentation.model;

import androidx.annotation.Nullable;

public class NoteListItem {

    public static final int TYPE_MODULE_FILE = 1;
    public static final int TYPE_PERSONAL_NOTE = 2;

    private final int type;
    private final String id;
    private final String title;
    private final String subtitle;
    private final String contentPreview;
    private final String storageUri;
    private final String folderId;
    private final String moduleId;

    public NoteListItem(int type, String id, String title, String subtitle,
                        String contentPreview, String storageUri) {
        this(type, id, title, subtitle, contentPreview, storageUri, null, null);
    }

    public NoteListItem(int type, String id, String title, String subtitle,
                        String contentPreview, String storageUri,
                        @Nullable String folderId) {
        this(type, id, title, subtitle, contentPreview, storageUri, folderId, null);
    }

    public NoteListItem(int type, String id, String title, String subtitle,
                        String contentPreview, String storageUri,
                        @Nullable String folderId,
                        @Nullable String moduleId) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.contentPreview = contentPreview;
        this.storageUri = storageUri;
        this.folderId = folderId;
        this.moduleId = moduleId;
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public String getStorageUri() {
        return storageUri;
    }

    @Nullable
    public String getFolderId() {
        return folderId;
    }

    @Nullable
    public String getModuleId() {
        return moduleId;
    }
}
