package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.CalendarEventEntity;

import java.util.List;

// [Data] Room DAO — all queries return LiveData so the View layer reacts automatically to changes.
@Dao
public interface CalendarEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CalendarEventEntity event);

    @Update
    void update(CalendarEventEntity event);

    @Delete
    void delete(CalendarEventEntity event);

    // Look up a single event by its UUID
    @Query("SELECT * FROM calendar_events WHERE eventId = :eventId")
    LiveData<CalendarEventEntity> getById(String eventId);

    // All events for a user, sorted by start time
    @Query("SELECT * FROM calendar_events WHERE userId = :userId ORDER BY startTime ASC")
    LiveData<List<CalendarEventEntity>> getAllByUser(String userId);

    // Events within a date range — the main query used to populate the calendar view
    @Query("SELECT * FROM calendar_events WHERE userId = :userId AND startTime BETWEEN :startMs AND :endMs ORDER BY startTime ASC")
    LiveData<List<CalendarEventEntity>> getByDateRange(String userId, long startMs, long endMs);

    // Events linked to a specific module
    @Query("SELECT * FROM calendar_events WHERE moduleId = :moduleId ORDER BY startTime ASC")
    LiveData<List<CalendarEventEntity>> getAllByModule(String moduleId);

    // Update shared fields (non-date) for every event in a recurrence group
    @Query("UPDATE calendar_events SET title = :title, description = :description, type = :type, moduleId = :moduleId, updatedAt = :updatedAt WHERE recurrenceGroupId = :groupId")
    void updateGroupFields(String groupId, String title, String description, String type, String moduleId, long updatedAt);

    // Delete all events belonging to a recurrence group
    @Query("DELETE FROM calendar_events WHERE recurrenceGroupId = :groupId")
    void deleteByGroupId(String groupId);
}
