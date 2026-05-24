package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// A to-do item. Optionally linked to a module.
@Entity(tableName = "todos",
        foreignKeys = @ForeignKey(
                entity = ModuleEntity.class,
                parentColumns = "moduleId",
                childColumns = "moduleId",
                onDelete = ForeignKey.SET_NULL), // deleting a module sets moduleId to null (no cascade)
        indices = {
                @Index({"userId", "isCompleted"}), // separate active vs. completed todos efficiently
                @Index("dueDate"),                  // sort by due date; used by calendar range queries
                @Index("moduleId"),                 // all todos for a specific module
                @Index("recurrenceGroupId")         // group deletes and updates by series
        })
public class TodoEntity {

    @PrimaryKey
    @NonNull
    private String todoId;       // UUID generated in the repository
    private String userId;       // owner
    private String moduleId;     // optional link to a module; null if not linked
    private String title;
    private String description;  // optional extra detail
    private Long dueDate;        // unix ms; null if no deadline
    private String priority;     // HIGH | MEDIUM | LOW
    private boolean isCompleted;
    private Long completedAt;          // unix ms when marked complete; null if still active
    private String recurrencePattern;  // DAILY | WEEKLY | MONTHLY | YEARLY; null = non-recurring
    private String recurrenceGroupId;  // shared UUID across all instances of a recurring series
    private Long recurrenceEndDate;    // unix ms; null if no end cap
    private long createdAt;            // unix ms
    private long updatedAt;            // unix ms

    public TodoEntity() {}

    @NonNull public String getTodoId() { return todoId; }
    public void setTodoId(@NonNull String todoId) { this.todoId = todoId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) { this.dueDate = dueDate; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
    public String getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(String recurrencePattern) { this.recurrencePattern = recurrencePattern; }
    public String getRecurrenceGroupId() { return recurrenceGroupId; }
    public void setRecurrenceGroupId(String recurrenceGroupId) { this.recurrenceGroupId = recurrenceGroupId; }
    public Long getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(Long recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
