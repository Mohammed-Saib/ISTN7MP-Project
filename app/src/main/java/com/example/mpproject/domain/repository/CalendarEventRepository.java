package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.CalendarEvent;

import java.util.List;

// [Domain] Contract for calendar event operations — implemented in the data layer, consumed by the ViewModel.
public interface CalendarEventRepository {
    void insert(CalendarEvent event);
    void insertBatch(List<CalendarEvent> events);
    void update(CalendarEvent event);
    void delete(CalendarEvent event);

    LiveData<CalendarEvent> getById(String eventId);

    // All events for a user, sorted by start time ascending
    LiveData<List<CalendarEvent>> getAllByUser(String userId);

    // Events within a date range — the main query used to populate the calendar view
    LiveData<List<CalendarEvent>> getByDateRange(String userId, long startMs, long endMs);

    // Events linked to a specific module
    LiveData<List<CalendarEvent>> getAllByModule(String moduleId);

    // Update shared non-date fields across every event in a recurrence group
    void updateGroup(CalendarEvent representative);

    // Delete all events in a recurrence group
    void deleteGroup(String recurrenceGroupId, String userId);
}
