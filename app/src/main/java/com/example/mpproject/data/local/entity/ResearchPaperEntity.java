package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "research_papers",
        indices = {
                @Index("userId"),
                @Index("status"),
                @Index("category"),
                @Index("authors"),
                @Index("year")
        }
)
public class ResearchPaperEntity {

    @PrimaryKey
    @NonNull
    private String paperId;

    private String userId;

    private String title;
    private String authors;
    private String year;
    private String category;

    private String summary;
    private String keyFindings;
    private String methodology;
    private String relevance;

    private String fileName;
    private String fileUrl;
    private String storagePath;

    private String status; // UNREAD, READING, READ
    private boolean important;

    private long uploadedAt;
    private long updatedAt;

    public ResearchPaperEntity() {
        this.paperId = "";
    }

    public ResearchPaperEntity(@NonNull String paperId,
                               String userId,
                               String title,
                               String authors,
                               String year,
                               String category,
                               String summary,
                               String keyFindings,
                               String methodology,
                               String relevance,
                               String fileName,
                               String fileUrl,
                               String storagePath,
                               String status,
                               boolean important,
                               long uploadedAt,
                               long updatedAt) {
        this.paperId = paperId;
        this.userId = userId;
        this.title = title;
        this.authors = authors;
        this.year = year;
        this.category = category;
        this.summary = summary;
        this.keyFindings = keyFindings;
        this.methodology = methodology;
        this.relevance = relevance;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.storagePath = storagePath;
        this.status = status;
        this.important = important;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(@NonNull String paperId) {
        this.paperId = paperId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors == null ? "" : authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getYear() {
        return year == null ? "" : year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSummary() {
        return summary == null ? "" : summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeyFindings() {
        return keyFindings == null ? "" : keyFindings;
    }

    public void setKeyFindings(String keyFindings) {
        this.keyFindings = keyFindings;
    }

    public String getMethodology() {
        return methodology == null ? "" : methodology;
    }

    public void setMethodology(String methodology) {
        this.methodology = methodology;
    }

    public String getRelevance() {
        return relevance == null ? "" : relevance;
    }

    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }

    public String getFileName() {
        return fileName == null ? "" : fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl == null ? "" : fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getStoragePath() {
        return storagePath == null ? "" : storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getStatus() {
        return status == null ? "UNREAD" : status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}