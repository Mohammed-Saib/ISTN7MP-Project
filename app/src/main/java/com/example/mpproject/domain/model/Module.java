package com.example.mpproject.domain.model;

// [Domain] Plain Java — no Room or Firebase annotations. Used by the ViewModel and Repository layers.
public class Module {
    private String moduleId;
    private String userId;
    private String name;
    private String moduleCode;
    private String lecturerName;
    private String lecturerEmail;
    private String lecturerOfficeHours;
    private String color;   // hex color string chosen by the user
    private String semester;
    private boolean isArchived;
    private long createdAt;
    private long updatedAt;

    public Module(String moduleId, String userId, String name) {
        this.moduleId = moduleId;
        this.userId = userId;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getModuleId() { return moduleId; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getModuleCode() { return moduleCode; }
    public String getLecturerName() { return lecturerName; }
    public String getLecturerEmail() { return lecturerEmail; }
    public String getLecturerOfficeHours() { return lecturerOfficeHours; }
    public String getColor() { return color; }
    public String getSemester() { return semester; }
    public boolean isArchived() { return isArchived; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }
    public void setLecturerEmail(String lecturerEmail) { this.lecturerEmail = lecturerEmail; }
    public void setLecturerOfficeHours(String lecturerOfficeHours) { this.lecturerOfficeHours = lecturerOfficeHours; }
    public void setColor(String color) { this.color = color; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setArchived(boolean archived) { isArchived = archived; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
