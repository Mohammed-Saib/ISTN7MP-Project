package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// A course or subject the user is enrolled in.
// Standalone — no university entity above it in this schema.
@Entity(tableName = "modules",
        indices = {@Index("userId")}) // fast query for all modules belonging to a user
public class ModuleEntity {

    @PrimaryKey
    @NonNull
    private String moduleId;             // UUID generated in the repository before insert
    private String userId;               // owner (Firebase UID)
    private String name;                 // e.g. 'Advanced Algorithms'
    private String moduleCode;           // e.g. 'CS3002', user-defined
    private String lecturerName;
    private String lecturerEmail;
    private String lecturerOfficeHours;  // free text, e.g. 'Mon 14:00–16:00'
    private String color;                // hex color for UI chip, e.g. '#2E75B6'
    private String semester;             // e.g. '2025-S1', free text
    private boolean isArchived;          // hidden from lists but not deleted when true
    private long createdAt;              // unix ms
    private long updatedAt;              // unix ms

    public ModuleEntity() {}

    @NonNull public String getModuleId() { return moduleId; }
    public void setModuleId(@NonNull String moduleId) { this.moduleId = moduleId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getLecturerName() { return lecturerName; }
    public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }
    public String getLecturerEmail() { return lecturerEmail; }
    public void setLecturerEmail(String lecturerEmail) { this.lecturerEmail = lecturerEmail; }
    public String getLecturerOfficeHours() { return lecturerOfficeHours; }
    public void setLecturerOfficeHours(String h) { this.lecturerOfficeHours = h; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
