package com.example.mpproject.domain.model;

// [Domain] Plain Java — no Room or Firebase annotations. A scheduled event; can optionally be pushed to the device calendar.
public class CalendarEvent {
    private String eventId;
    private String userId;
    private String moduleId;              // nullable — not every event is module-related
    private String title;
    private String description;
    private String type;                  // e.g. "LECTURE", "EXAM", "DEADLINE", "OTHER"
    private String color;                 // hex color for the calendar chip
    private long startTime;              // epoch millis
    private Long endTime;                // nullable — null for point-in-time events
    private boolean isAllDay;
    private boolean isPushedToDeviceCalendar;
    private Long deviceCalendarEventId;  // nullable — ID returned by CalendarContract after push
    private long createdAt;
    private long updatedAt;

    public CalendarEvent(String eventId, String userId, String title, long startTime) {
        this.eventId = eventId;
        this.userId = userId;
        this.title = title;
        this.startTime = startTime;
        this.type = "OTHER";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public String getModuleId() { return moduleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getColor() { return color; }
    public long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public boolean isAllDay() { return isAllDay; }
    public boolean isPushedToDeviceCalendar() { return isPushedToDeviceCalendar; }
    public Long getDeviceCalendarEventId() { return deviceCalendarEventId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setColor(String color) { this.color = color; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setAllDay(boolean allDay) { isAllDay = allDay; }
    public void setPushedToDeviceCalendar(boolean pushed) { isPushedToDeviceCalendar = pushed; }
    public void setDeviceCalendarEventId(Long deviceCalendarEventId) { this.deviceCalendarEventId = deviceCalendarEventId; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
