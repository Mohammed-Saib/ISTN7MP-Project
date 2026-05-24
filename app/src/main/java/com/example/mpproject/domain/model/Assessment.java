package com.example.mpproject.domain.model;

// [Domain] Plain Java — no Room or Firebase annotations. Represents a module assessment entry.
public class Assessment {
    private String assessmentId;     // UUID PK
    private String moduleId;         // FK → modules
    private String userId;
    private String title;
    private String assessmentType;   // QUIZ | TEST | EXAM | ASSIGNMENT | LAB | OTHER
    private double weightingPercent; // 0–100
    private String scoreMode;        // PERCENTAGE | RAW
    private Double scoreAchieved;    // null = not yet graded
    private Double scoreMaximum;     // denominator for RAW mode (e.g. 85 for "72/85")
    private String calendarEventId;  // UUID of the auto-created calendar event; null if no date
    private Long dueDate;            // epoch ms; null = no date
    private String notes;
    private long createdAt;
    private long updatedAt;

    public Assessment(String assessmentId, String moduleId, String userId, String title) {
        this.assessmentId    = assessmentId;
        this.moduleId        = moduleId;
        this.userId          = userId;
        this.title           = title;
        this.scoreMode       = "PERCENTAGE";
        this.createdAt       = System.currentTimeMillis();
        this.updatedAt       = System.currentTimeMillis();
    }

    // ── Computed helpers ─────────────────────────────────────────────────────

    public boolean isGraded() { return scoreAchieved != null; }

    // Returns null if not graded.
    public Double getScorePercent() {
        if (scoreAchieved == null) return null;
        if ("RAW".equals(scoreMode) && scoreMaximum != null && scoreMaximum > 0) {
            return scoreAchieved / scoreMaximum * 100.0;
        }
        return scoreAchieved; // PERCENTAGE mode
    }

    // Weighted contribution to the final grade — null if not graded.
    public Double getContributionPercent() {
        Double pct = getScorePercent();
        if (pct == null) return null;
        return pct * (weightingPercent / 100.0);
    }

    // ── Getters / setters ────────────────────────────────────────────────────

    public String getAssessmentId()   { return assessmentId; }
    public String getModuleId()        { return moduleId; }
    public String getUserId()          { return userId; }
    public String getTitle()           { return title; }
    public String getAssessmentType()  { return assessmentType; }
    public double getWeightingPercent(){ return weightingPercent; }
    public String getScoreMode()       { return scoreMode; }
    public Double getScoreAchieved()   { return scoreAchieved; }
    public Double getScoreMaximum()    { return scoreMaximum; }
    public String getCalendarEventId() { return calendarEventId; }
    public Long   getDueDate()         { return dueDate; }
    public String getNotes()           { return notes; }
    public long   getCreatedAt()       { return createdAt; }
    public long   getUpdatedAt()       { return updatedAt; }

    public void setAssessmentId(String assessmentId)     { this.assessmentId    = assessmentId; }
    public void setModuleId(String moduleId)             { this.moduleId        = moduleId; }
    public void setUserId(String userId)                 { this.userId          = userId; }
    public void setTitle(String title)                   { this.title           = title; }
    public void setAssessmentType(String type)           { this.assessmentType  = type; }
    public void setWeightingPercent(double w)            { this.weightingPercent = w; }
    public void setScoreMode(String scoreMode)           { this.scoreMode       = scoreMode; }
    public void setScoreAchieved(Double scoreAchieved)   { this.scoreAchieved  = scoreAchieved; }
    public void setScoreMaximum(Double scoreMaximum)     { this.scoreMaximum    = scoreMaximum; }
    public void setCalendarEventId(String id)            { this.calendarEventId = id; }
    public void setDueDate(Long dueDate)                 { this.dueDate         = dueDate; }
    public void setNotes(String notes)                   { this.notes           = notes; }
    public void setCreatedAt(long createdAt)             { this.createdAt       = createdAt; }
    public void setUpdatedAt(long updatedAt)             { this.updatedAt       = updatedAt; }
}
