package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.AssessmentEntity;

import java.util.List;

// [Data] Room DAO for the assessments table.
@Dao
public interface AssessmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AssessmentEntity assessment);

    @Update
    void update(AssessmentEntity assessment);

    @Delete
    void delete(AssessmentEntity assessment);

    @Query("SELECT * FROM assessments WHERE moduleId = :moduleId ORDER BY dueDate ASC")
    LiveData<List<AssessmentEntity>> getByModule(String moduleId);

    @Query("SELECT * FROM assessments WHERE userId = :userId")
    LiveData<List<AssessmentEntity>> getByUser(String userId);

    @Query("UPDATE assessments SET calendarEventId = NULL WHERE assessmentId = :assessmentId")
    void clearCalendarEventId(String assessmentId);

    @Query("SELECT * FROM assessments WHERE assessmentId = :assessmentId LIMIT 1")
    AssessmentEntity getByIdSync(String assessmentId);

    @Query("UPDATE assessments SET dueDate = :dueDate, notes = :notes, updatedAt = :updatedAt WHERE assessmentId = :assessmentId")
    void updateDueDateAndNotes(String assessmentId, Long dueDate, String notes, long updatedAt);
}
