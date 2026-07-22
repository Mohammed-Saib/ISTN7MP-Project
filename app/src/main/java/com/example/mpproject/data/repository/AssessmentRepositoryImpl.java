package com.example.mpproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.AssessmentDao;
import com.example.mpproject.data.local.entity.AssessmentEntity;
import com.example.mpproject.domain.model.Assessment;
import com.example.mpproject.domain.repository.AssessmentRepository;

import java.util.ArrayList;
import java.util.List;

public class AssessmentRepositoryImpl implements AssessmentRepository {

    private final AssessmentDao assessmentDao;

    public AssessmentRepositoryImpl(AssessmentDao assessmentDao) {
        this.assessmentDao = assessmentDao;
    }

    @Override
    public void insert(Assessment assessment) {
        AppDatabase.databaseWriteExecutor.execute(() -> assessmentDao.insert(toEntity(assessment)));
    }

    @Override
    public void update(Assessment assessment) {
        AppDatabase.databaseWriteExecutor.execute(() -> assessmentDao.update(toEntity(assessment)));
    }

    @Override
    public void delete(Assessment assessment) {
        AppDatabase.databaseWriteExecutor.execute(() -> assessmentDao.delete(toEntity(assessment)));
    }

    @Override
    public LiveData<List<Assessment>> getByModule(String moduleId) {
        return Transformations.map(assessmentDao.getByModule(moduleId), this::toDomainList);
    }

    @Override
    public LiveData<List<Assessment>> getByUser(String userId) {
        return Transformations.map(assessmentDao.getByUser(userId), this::toDomainList);
    }

    @Override
    public void clearCalendarEventId(String assessmentId) {
        AppDatabase.databaseWriteExecutor.execute(() -> assessmentDao.clearCalendarEventId(assessmentId));
    }

    public void updateDueDateAndNotes(String assessmentId, Long dueDate, String notes) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            assessmentDao.updateDueDateAndNotes(assessmentId, dueDate, notes, now);
        });
    }

    private Assessment toDomain(AssessmentEntity e) {
        if (e == null) return null;
        Assessment a = new Assessment(e.getAssessmentId(), e.getModuleId(), e.getUserId(), e.getTitle());
        a.setAssessmentType(e.getAssessmentType());
        a.setWeightingPercent(e.getWeightingPercent());
        a.setScoreMode(e.getScoreMode());
        a.setScoreAchieved(e.getScoreAchieved());
        a.setScoreMaximum(e.getScoreMaximum());
        a.setCalendarEventId(e.getCalendarEventId());
        a.setDueDate(e.getDueDate());
        a.setNotes(e.getNotes());
        a.setCreatedAt(e.getCreatedAt());
        a.setUpdatedAt(e.getUpdatedAt());
        return a;
    }

    private List<Assessment> toDomainList(List<AssessmentEntity> entities) {
        List<Assessment> list = new ArrayList<>();
        if (entities != null) for (AssessmentEntity e : entities) list.add(toDomain(e));
        return list;
    }

    private AssessmentEntity toEntity(Assessment a) {
        AssessmentEntity e = new AssessmentEntity();
        e.setAssessmentId(a.getAssessmentId());
        e.setModuleId(a.getModuleId());
        e.setUserId(a.getUserId());
        e.setTitle(a.getTitle());
        e.setAssessmentType(a.getAssessmentType());
        e.setWeightingPercent(a.getWeightingPercent());
        e.setScoreMode(a.getScoreMode());
        e.setScoreAchieved(a.getScoreAchieved());
        e.setScoreMaximum(a.getScoreMaximum());
        e.setCalendarEventId(a.getCalendarEventId());
        e.setDueDate(a.getDueDate());
        e.setNotes(a.getNotes());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }
}
