package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.Assessment;

import java.util.List;

// [Domain] Repository contract for assessment CRUD and calendar event link clearing.
public interface AssessmentRepository {
    void insert(Assessment assessment);
    void update(Assessment assessment);
    void delete(Assessment assessment);
    LiveData<List<Assessment>> getByModule(String moduleId);
    LiveData<List<Assessment>> getByUser(String userId);
    void clearCalendarEventId(String assessmentId);
}
