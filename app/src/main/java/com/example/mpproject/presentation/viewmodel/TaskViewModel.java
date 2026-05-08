package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.domain.repository.TodoRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.UUID;

// [ViewModel] Manages the todo list for the logged-in user; survives configuration changes.
public class TaskViewModel extends ViewModel {

    private final TodoRepository todoRepository;
    private final LiveData<List<Todo>> allTodos;
    private final String userId;

    public TaskViewModel(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
        // Get the current user's UID so all queries are scoped to this user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        this.allTodos = todoRepository.getAllByUser(userId);
    }

    // Returns only active (non-completed) todos for the current user
    public LiveData<List<Todo>> getAllTasks() {
        return allTodos;
    }

    public void addTask() {
        Todo todo = new Todo(UUID.randomUUID().toString(), userId, "New Task");
        todoRepository.insert(todo);
    }

    public void updateTask(Todo todo) {
        todoRepository.update(todo);
    }

    public void deleteTask(Todo todo) {
        todoRepository.delete(todo);
    }
}
