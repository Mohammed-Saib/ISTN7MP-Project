package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.Todo;

import java.util.List;

// [Domain] Contract for todo operations — implemented in the data layer, consumed by the ViewModel.
public interface TodoRepository {
    void insert(Todo todo);
    void update(Todo todo);
    void delete(Todo todo);

    LiveData<Todo> getById(String todoId);

    // Active (not completed) todos for a user, sorted by due date ascending
    LiveData<List<Todo>> getAllByUser(String userId);

    // Completed todos, most recently completed first
    LiveData<List<Todo>> getCompletedByUser(String userId);

    // Todos linked to a specific module
    LiveData<List<Todo>> getAllByModule(String moduleId);

    // Todos with a due date in a given range — used by the calendar screen
    LiveData<List<Todo>> getByDueDateRange(String userId, long startMs, long endMs);
}
