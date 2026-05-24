package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.AssessmentDao;
import com.example.mpproject.data.local.entity.AssessmentEntity;
import com.example.mpproject.domain.model.Assessment;
import com.example.mpproject.domain.repository.AssessmentRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Implements AssessmentRepository. Room is the source of truth; Firestore synced in background.
public class AssessmentRepositoryImpl implements AssessmentRepository {

    private static final String TAG = "AssessmentRepository";

    private final AssessmentDao assessmentDao;
    private final FirebaseFirestore firestore;

    public AssessmentRepositoryImpl(AssessmentDao assessmentDao) {
        this.assessmentDao = assessmentDao;
        this.firestore     = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(Assessment assessment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            assessmentDao.insert(toEntity(assessment));
            syncToFirestore(assessment);
        });
    }

    @Override
    public void update(Assessment assessment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            assessmentDao.update(toEntity(assessment));
            syncToFirestore(assessment);
        });
    }

    @Override
    public void delete(Assessment assessment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            assessmentDao.delete(toEntity(assessment));
            try {
                firestore.collection("users").document(assessment.getUserId())
                        .collection("assessments").document(assessment.getAssessmentId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore delete error", e);
            }
        });
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
        AppDatabase.databaseWriteExecutor.execute(() -> {
            assessmentDao.clearCalendarEventId(assessmentId);
            // Patch Firestore: we need userId to construct the path; fetch entity first
            AssessmentEntity e = assessmentDao.getByIdSync(assessmentId);
            if (e == null || e.getUserId() == null) return;
            try {
                Map<String, Object> patch = new HashMap<>();
                patch.put("calendarEventId", null);
                firestore.collection("users").document(e.getUserId())
                        .collection("assessments").document(assessmentId)
                        .update(patch)
                        .addOnFailureListener(ex -> Log.e(TAG, "Firestore clearCalendarEventId failed", ex));
            } catch (Exception ex) {
                Log.e(TAG, "Firestore clearCalendarEventId error", ex);
            }
        });
    }

    // Update dueDate and notes on an assessment from the calendar side (calendar → assessment sync).
    public void updateDueDateAndNotes(String assessmentId, Long dueDate, String notes) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            assessmentDao.updateDueDateAndNotes(assessmentId, dueDate, notes, now);
            AssessmentEntity e = assessmentDao.getByIdSync(assessmentId);
            if (e == null || e.getUserId() == null) return;
            try {
                Map<String, Object> patch = new HashMap<>();
                patch.put("dueDate",   dueDate);
                patch.put("notes",     notes);
                patch.put("updatedAt", now);
                firestore.collection("users").document(e.getUserId())
                        .collection("assessments").document(assessmentId)
                        .update(patch)
                        .addOnFailureListener(ex -> Log.e(TAG, "Firestore updateDueDateAndNotes failed", ex));
            } catch (Exception ex) {
                Log.e(TAG, "Firestore updateDueDateAndNotes error", ex);
            }
        });
    }

    // Pull all assessments for this user from Firestore and upsert into Room.
    public void syncFromFirestore(String userId) {
        firestore.collection("users").document(userId)
                .collection("assessments")
                .get()
                .addOnSuccessListener(snap ->
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                try {
                                    AssessmentEntity e = new AssessmentEntity();
                                    e.setAssessmentId(doc.getId());
                                    e.setModuleId(doc.getString("moduleId"));
                                    e.setUserId(userId);
                                    e.setTitle(doc.getString("title"));
                                    e.setAssessmentType(doc.getString("assessmentType"));
                                    Double w = doc.getDouble("weightingPercent");
                                    e.setWeightingPercent(w != null ? w : 0.0);
                                    e.setScoreMode(doc.getString("scoreMode"));
                                    e.setScoreAchieved(doc.getDouble("scoreAchieved"));
                                    e.setScoreMaximum(doc.getDouble("scoreMaximum"));
                                    e.setCalendarEventId(doc.getString("calendarEventId"));
                                    e.setDueDate(doc.getLong("dueDate"));
                                    e.setNotes(doc.getString("notes"));
                                    Long createdAt = doc.getLong("createdAt");
                                    e.setCreatedAt(createdAt != null ? createdAt : 0L);
                                    Long updatedAt = doc.getLong("updatedAt");
                                    e.setUpdatedAt(updatedAt != null ? updatedAt : 0L);
                                    assessmentDao.insert(e);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error syncing assessment from Firestore", ex);
                                }
                            }
                        }))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore assessment sync failed", e));
    }

    private void syncToFirestore(Assessment a) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("moduleId",        a.getModuleId());
            data.put("userId",          a.getUserId());
            data.put("title",           a.getTitle());
            data.put("assessmentType",  a.getAssessmentType());
            data.put("weightingPercent",a.getWeightingPercent());
            data.put("scoreMode",       a.getScoreMode());
            data.put("scoreAchieved",   a.getScoreAchieved());
            data.put("scoreMaximum",    a.getScoreMaximum());
            data.put("calendarEventId", a.getCalendarEventId());
            data.put("dueDate",         a.getDueDate());
            data.put("notes",           a.getNotes());
            data.put("createdAt",       a.getCreatedAt());
            data.put("updatedAt",       a.getUpdatedAt());

            firestore.collection("users").document(a.getUserId())
                    .collection("assessments").document(a.getAssessmentId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
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
