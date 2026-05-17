package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.TodoEntity;

import java.util.List;

// [Data] Room DAO — all queries return LiveData so the View layer reacts automatically to changes.
@Dao
public interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TodoEntity todo);

    @Update
    void update(TodoEntity todo);

    @Delete
    void delete(TodoEntity todo);

    // Look up a single todo by its UUID
    @Query("SELECT * FROM todos WHERE todoId = :todoId")
    LiveData<TodoEntity> getById(String todoId);

    // Active (not completed) todos for a user, sorted by due date ascending (earliest first)
    @Query("SELECT * FROM todos WHERE userId = :userId AND isCompleted = 0 ORDER BY dueDate ASC")
    LiveData<List<TodoEntity>> getAllByUser(String userId);

    // Completed todos for a user, most recently completed first
    @Query("SELECT * FROM todos WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    LiveData<List<TodoEntity>> getCompletedByUser(String userId);

    // Todos linked to a specific module
    @Query("SELECT * FROM todos WHERE moduleId = :moduleId ORDER BY dueDate ASC")
    LiveData<List<TodoEntity>> getAllByModule(String moduleId);

    // Todos with a due date in a given range — used by the calendar screen
    @Query("SELECT * FROM todos WHERE userId = :userId AND dueDate BETWEEN :startMs AND :endMs")
    LiveData<List<TodoEntity>> getByDueDateRange(String userId, long startMs, long endMs);

    // Update shared fields (non-date) for every todo in a recurrence group
    @Query("UPDATE todos SET title = :title, description = :description, priority = :priority, moduleId = :moduleId, updatedAt = :updatedAt WHERE recurrenceGroupId = :groupId")
    void updateGroupFields(String groupId, String title, String description, String priority, String moduleId, long updatedAt);

    // Delete all todos belonging to a recurrence group
    @Query("DELETE FROM todos WHERE recurrenceGroupId = :groupId")
    void deleteByGroupId(String groupId);

    @Query("SELECT COUNT(*) FROM todos WHERE userId = :userId")
    int countByUser(String userId);
}
