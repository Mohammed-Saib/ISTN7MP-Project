package com.example.mpproject.domain.model;

// [Domain] Plain Java model — no Android or Firebase dependencies.
public class Todo {
    private String todoId;
    private String userId;
    private String moduleId;
    private String title;
    private String description;
    private String priority;           // "LOW", "MEDIUM", "HIGH"
    private Long dueDate;              // nullable epoch millis
    private Long completedAt;          // nullable — set when the todo is marked complete
    private boolean isCompleted;
    private String recurrencePattern;  // DAILY | WEEKLY | MONTHLY | YEARLY; null = non-recurring
    private String recurrenceGroupId;  // shared UUID linking all occurrences in a series
    private Long recurrenceEndDate;    // nullable epoch millis — end of recurrence window
    private long createdAt;
    private long updatedAt;

    public Todo(String todoId, String userId, String title) {
        this.todoId = todoId;
        this.userId = userId;
        this.title = title;
        this.priority = "MEDIUM";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getTodoId() { return todoId; }
    public String getUserId() { return userId; }
    public String getModuleId() { return moduleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public Long getDueDate() { return dueDate; }
    public Long getCompletedAt() { return completedAt; }
    public boolean isCompleted() { return isCompleted; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDueDate(Long dueDate) { this.dueDate = dueDate; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Set completion state. Callers must also set completedAt explicitly (avoids the
    // repository accidentally overwriting the stored timestamp with System.currentTimeMillis()).
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public String getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(String recurrencePattern) { this.recurrencePattern = recurrencePattern; }

    public String getRecurrenceGroupId() { return recurrenceGroupId; }
    public void setRecurrenceGroupId(String recurrenceGroupId) { this.recurrenceGroupId = recurrenceGroupId; }

    public Long getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(Long recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }
}
