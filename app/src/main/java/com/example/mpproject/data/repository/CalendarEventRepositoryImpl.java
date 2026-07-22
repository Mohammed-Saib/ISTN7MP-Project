package com.example.mpproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.CalendarEventDao;
import com.example.mpproject.data.local.entity.CalendarEventEntity;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.repository.CalendarEventRepository;

import java.util.ArrayList;
import java.util.List;

public class CalendarEventRepositoryImpl implements CalendarEventRepository {

    private final CalendarEventDao calendarEventDao;

    public CalendarEventRepositoryImpl(CalendarEventDao calendarEventDao) {
        this.calendarEventDao = calendarEventDao;
    }

    @Override
    public void insert(CalendarEvent event) {
        AppDatabase.databaseWriteExecutor.execute(() -> calendarEventDao.insert(toEntity(event)));
    }

    @Override
    public void insertBatch(List<CalendarEvent> events) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<CalendarEventEntity> entities = new ArrayList<>();
            for (CalendarEvent e : events) entities.add(toEntity(e));
            calendarEventDao.insertAll(entities);
        });
    }

    @Override
    public void update(CalendarEvent event) {
        AppDatabase.databaseWriteExecutor.execute(() -> calendarEventDao.update(toEntity(event)));
    }

    @Override
    public void delete(CalendarEvent event) {
        AppDatabase.databaseWriteExecutor.execute(() -> calendarEventDao.delete(toEntity(event)));
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
        });
    }

    @Override
    public void deleteGroup(String recurrenceGroupId, String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> calendarEventDao.deleteByGroupId(recurrenceGroupId));
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
        event.setLinkedAssessmentId(e.getLinkedAssessmentId());
        event.setCreatedAt(e.getCreatedAt());
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
        e.setLinkedAssessmentId(event.getLinkedAssessmentId());
        e.setCreatedAt(event.getCreatedAt());
        e.setUpdatedAt(event.getUpdatedAt());
        return e;
    }
}
