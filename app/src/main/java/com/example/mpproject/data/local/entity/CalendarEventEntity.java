package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// A calendar event (lecture, exam, personal appointment).
// Can optionally be pushed to the device's native calendar via CalendarContract.
@Entity(tableName = "calendar_events",
        foreignKeys = @ForeignKey(
                entity = ModuleEntity.class,
                parentColumns = "moduleId",
                childColumns = "moduleId",
                onDelete = ForeignKey.SET_NULL), // deleting a module sets moduleId null (no cascade)
        indices = {
                @Index({"userId", "startTime"}), // calendar range queries — most common read
                @Index("moduleId")               // all events for a specific module
        })
public class CalendarEventEntity {

    @PrimaryKey
    @NonNull
    private String eventId;                   // UUID
    private String userId;                    // owner
    private String moduleId;                  // optional link; null if not linked
    private String title;                     // e.g. 'CS3002 Lecture' or 'Group Study'
    private String description;               // optional notes about the event
    private long startTime;                   // unix ms
    private Long endTime;                     // unix ms; null for all-day or single-point events
    private boolean isAllDay;                 // when true, endTime is ignored
    private String type;                      // LECTURE | EXAM | ASSIGNMENT_DUE | PERSONAL
    private String color;                     // hex override; falls back to module color if null
    private Long deviceCalendarEventId;       // ID from CalendarContract after a device push; null if not pushed
    private boolean isPushedToDeviceCalendar; // true after a successful CalendarContract insert
    private long createdAt;                   // unix ms
    private long updatedAt;                   // unix ms

    public CalendarEventEntity() {}

    @NonNull public String getEventId() { return eventId; }
    public void setEventId(@NonNull String eventId) { this.eventId = eventId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public boolean isAllDay() { return isAllDay; }
    public void setAllDay(boolean allDay) { isAllDay = allDay; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Long getDeviceCalendarEventId() { return deviceCalendarEventId; }
    public void setDeviceCalendarEventId(Long id) { this.deviceCalendarEventId = id; }
    public boolean isPushedToDeviceCalendar() { return isPushedToDeviceCalendar; }
    public void setPushedToDeviceCalendar(boolean pushed) { isPushedToDeviceCalendar = pushed; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
