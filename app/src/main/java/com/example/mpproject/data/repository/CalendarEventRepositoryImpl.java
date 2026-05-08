package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.CalendarEventDao;
import com.example.mpproject.data.local.entity.CalendarEventEntity;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.repository.CalendarEventRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Implements CalendarEventRepository. Room is the source of truth; Firestore is synced in the background.
public class CalendarEventRepositoryImpl implements CalendarEventRepository {

    private static final String TAG = "CalendarEventRepository";

    private final CalendarEventDao calendarEventDao;
    private final FirebaseFirestore firestore;

    public CalendarEventRepositoryImpl(CalendarEventDao calendarEventDao) {
        this.calendarEventDao = calendarEventDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(CalendarEvent event) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            calendarEventDao.insert(toEntity(event));
            syncToFirestore(event);
        });
    }

    @Override
    public void update(CalendarEvent event) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            calendarEventDao.update(toEntity(event));
            syncToFirestore(event);
        });
    }

    @Override
    public void delete(CalendarEvent event) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            calendarEventDao.delete(toEntity(event));
            try {
                firestore.collection("users").document(event.getUserId())
                        .collection("calendar_events").document(event.getEventId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore delete error", e);
            }
        });
    }

    @Override
    public LiveData<CalendarEvent> getById(String eventId) {
        return Transformations.map(calendarEventDao.getById(eventId), this::toDomain);
    }

    @Override
    public LiveData<List<CalendarEvent>> getAllByUser(String userId) {
        return Transformations.map(calendarEventDao.getAllByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<CalendarEvent>> getByDateRange(String userId, long startMs, long endMs) {
        return Transformations.map(calendarEventDao.getByDateRange(userId, startMs, endMs), this::toDomainList);
    }

    @Override
    public LiveData<List<CalendarEvent>> getAllByModule(String moduleId) {
        return Transformations.map(calendarEventDao.getAllByModule(moduleId), this::toDomainList);
    }

    // Sync to users/{userId}/calendar_events/{eventId} in Firestore
    private void syncToFirestore(CalendarEvent event) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("title", event.getTitle());
            data.put("description", event.getDescription());
            data.put("moduleId", event.getModuleId());
            data.put("type", event.getType());
            data.put("color", event.getColor());
            data.put("startTime", event.getStartTime());
            data.put("endTime", event.getEndTime());
            data.put("isAllDay", event.isAllDay());
            data.put("isPushedToDeviceCalendar", event.isPushedToDeviceCalendar());
            data.put("deviceCalendarEventId", event.getDeviceCalendarEventId());
            data.put("createdAt", event.getCreatedAt());
            data.put("updatedAt", event.getUpdatedAt());

            firestore.collection("users").document(event.getUserId())
                    .collection("calendar_events").document(event.getEventId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
    }

    private CalendarEvent toDomain(CalendarEventEntity e) {
        if (e == null) return null;
        CalendarEvent event = new CalendarEvent(e.getEventId(), e.getUserId(),
                e.getTitle(), e.getStartTime());
        event.setModuleId(e.getModuleId());
        event.setDescription(e.getDescription());
        event.setType(e.getType());
        event.setColor(e.getColor());
        event.setEndTime(e.getEndTime());
        event.setAllDay(e.isAllDay());
        event.setPushedToDeviceCalendar(e.isPushedToDeviceCalendar());
        event.setDeviceCalendarEventId(e.getDeviceCalendarEventId());
        event.setUpdatedAt(e.getUpdatedAt());
        return event;
    }

    private List<CalendarEvent> toDomainList(List<CalendarEventEntity> entities) {
        List<CalendarEvent> list = new ArrayList<>();
        if (entities != null) {
            for (CalendarEventEntity e : entities) list.add(toDomain(e));
        }
        return list;
    }

    private CalendarEventEntity toEntity(CalendarEvent event) {
        CalendarEventEntity e = new CalendarEventEntity();
        e.setEventId(event.getEventId());
        e.setUserId(event.getUserId());
        e.setModuleId(event.getModuleId());
        e.setTitle(event.getTitle());
        e.setDescription(event.getDescription());
        e.setType(event.getType());
        e.setColor(event.getColor());
        e.setStartTime(event.getStartTime());
        e.setEndTime(event.getEndTime());
        e.setAllDay(event.isAllDay());
        e.setPushedToDeviceCalendar(event.isPushedToDeviceCalendar());
        e.setDeviceCalendarEventId(event.getDeviceCalendarEventId());
        e.setCreatedAt(event.getCreatedAt());
        e.setUpdatedAt(event.getUpdatedAt());
        return e;
    }
}
