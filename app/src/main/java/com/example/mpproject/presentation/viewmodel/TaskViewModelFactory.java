package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.TodoRepository;

public class TaskViewModelFactory implements ViewModelProvider.Factory {

    private final TodoRepository todoRepository;
    private final ModuleRepository moduleRepository;
    private final String userId;

    public TaskViewModelFactory(TodoRepository todoRepository, ModuleRepository moduleRepository, String userId) {
        this.todoRepository = todoRepository;
        this.moduleRepository = moduleRepository;
        this.userId = userId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TaskViewModel.class)) {
            return (T) new TaskViewModel(todoRepository, moduleRepository, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
