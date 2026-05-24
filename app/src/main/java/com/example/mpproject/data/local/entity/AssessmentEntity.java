package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "assessments",
        foreignKeys = @ForeignKey(
                entity = ModuleEntity.class,
                parentColumns = "moduleId",
                childColumns = "moduleId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("moduleId"),
                @Index("userId")
        })
// [Data] Room entity for the assessments table; CASCADE FK to modules.
public class AssessmentEntity {

    @PrimaryKey
    @NonNull
    private String assessmentId;
    private String moduleId;
    private String userId;
    private String title;
    private String assessmentType;
    private double weightingPercent;
    private String scoreMode;
    private Double scoreAchieved;
    private Double scoreMaximum;
    private String calendarEventId;
    private Long dueDate;
    private String notes;
    private long createdAt;
    private long updatedAt;

    public AssessmentEntity() {}

    @NonNull public String getAssessmentId()    { return assessmentId; }
    public void setAssessmentId(@NonNull String id) { this.assessmentId = id; }
    public String getModuleId()                 { return moduleId; }
    public void setModuleId(String moduleId)    { this.moduleId = moduleId; }
    public String getUserId()                   { return userId; }
    public void setUserId(String userId)        { this.userId = userId; }
    public String getTitle()                    { return title; }
    public void setTitle(String title)          { this.title = title; }
    public String getAssessmentType()           { return assessmentType; }
    public void setAssessmentType(String t)     { this.assessmentType = t; }
    public double getWeightingPercent()         { return weightingPercent; }
    public void setWeightingPercent(double w)   { this.weightingPercent = w; }
    public String getScoreMode()                { return scoreMode; }
    public void setScoreMode(String s)          { this.scoreMode = s; }
    public Double getScoreAchieved()            { return scoreAchieved; }
    public void setScoreAchieved(Double s)      { this.scoreAchieved = s; }
    public Double getScoreMaximum()             { return scoreMaximum; }
    public void setScoreMaximum(Double m)       { this.scoreMaximum = m; }
    public String getCalendarEventId()          { return calendarEventId; }
    public void setCalendarEventId(String id)   { this.calendarEventId = id; }
    public Long getDueDate()                    { return dueDate; }
    public void setDueDate(Long d)              { this.dueDate = d; }
    public String getNotes()                    { return notes; }
    public void setNotes(String notes)          { this.notes = notes; }
    public long getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(long t)            { this.createdAt = t; }
    public long getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(long t)            { this.updatedAt = t; }
}
