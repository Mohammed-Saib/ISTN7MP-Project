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

    @Override
    public void updateGroup(CalendarEvent representative) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            calendarEventDao.updateGroupFields(
                    representative.getRecurrenceGroupId(),
                    representative.getTitle(),
                    representative.getDescription(),
                    representative.getType(),
                    representative.getModuleId(),
                    now);
            syncGroupToFirestore(representative, now);
        });
    }

    @Override
    public void deleteGroup(String recurrenceGroupId, String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            calendarEventDao.deleteByGroupId(recurrenceGroupId);
            try {
                firestore.collection("users").document(userId)
                        .collection("calendar_events")
                        .whereEqualTo("recurrenceGroupId", recurrenceGroupId)
                        .get()
                        .addOnSuccessListener(snap -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                doc.getReference().delete();
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore group delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore group delete error", e);
            }
        });
    }

    // If Room has no events for this user, fetch them all from Firestore and insert.
    public void syncFromFirestoreIfEmpty(String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (calendarEventDao.countByUser(userId) > 0) return;
            firestore.collection("users").document(userId)
                    .collection("calendar_events")
                    .get()
                    .addOnSuccessListener(snap ->
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                    try {
                                        CalendarEventEntity e = new CalendarEventEntity();
                                        e.setEventId(doc.getId());
                                        e.setUserId(userId);
                                        e.setTitle(doc.getString("title"));
                                        e.setDescription(doc.getString("description"));
                                        e.setModuleId(doc.getString("moduleId"));
                                        e.setType(doc.getString("type"));
                                        e.setColor(doc.getString("color"));
                                        Long startTime = doc.getLong("startTime");
                                        e.setStartTime(startTime != null ? startTime : 0L);
                                        e.setEndTime(doc.getLong("endTime"));
                                        Boolean allDay = doc.getBoolean("isAllDay");
                                        e.setAllDay(allDay != null && allDay);
                                        Boolean pushed = doc.getBoolean("isPushedToDeviceCalendar");
                                        e.setPushedToDeviceCalendar(pushed != null && pushed);
                                        e.setDeviceCalendarEventId(doc.getLong("deviceCalendarEventId"));
                                        e.setRecurrencePattern(doc.getString("recurrencePattern"));
                                        e.setRecurrenceGroupId(doc.getString("recurrenceGroupId"));
                                        e.setRecurrenceEndDate(doc.getLong("recurrenceEndDate"));
                                        Long createdAt = doc.getLong("createdAt");
                                        e.setCreatedAt(createdAt != null ? createdAt : 0L);
                                        Long updatedAt = doc.getLong("updatedAt");
                                        e.setUpdatedAt(updatedAt != null ? updatedAt : 0L);
                                        calendarEventDao.insert(e);
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error restoring event from Firestore", ex);
                                    }
                                }
                            }))
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore event restore failed", e));
        });
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
            data.put("recurrencePattern", event.getRecurrencePattern());
            data.put("recurrenceGroupId", event.getRecurrenceGroupId());
            data.put("recurrenceEndDate", event.getRecurrenceEndDate());
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

    private void syncGroupToFirestore(CalendarEvent representative, long updatedAt) {
        try {
            firestore.collection("users").document(representative.getUserId())
                    .collection("calendar_events")
                    .whereEqualTo("recurrenceGroupId", representative.getRecurrenceGroupId())
                    .get()
                    .addOnSuccessListener(snap -> {
                        Map<String, Object> patch = new HashMap<>();
                        patch.put("title", representative.getTitle());
                        patch.put("description", representative.getDescription());
                        patch.put("type", representative.getType());
                        patch.put("moduleId", representative.getModuleId());
                        patch.put("updatedAt", updatedAt);
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            doc.getReference().update(patch);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore group sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore group sync error", e);
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
        event.setRecurrencePattern(e.getRecurrencePattern());
        event.setRecurrenceGroupId(e.getRecurrenceGroupId());
        event.setRecurrenceEndDate(e.getRecurrenceEndDate());
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
        e.setRecurrencePattern(event.getRecurrencePattern());
        e.setRecurrenceGroupId(event.getRecurrenceGroupId());
        e.setRecurrenceEndDate(event.getRecurrenceEndDate());
        e.setCreatedAt(event.getCreatedAt());
        e.setUpdatedAt(event.getUpdatedAt());
        return e;
    }
}
