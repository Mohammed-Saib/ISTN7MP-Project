package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.TodoRepository;

// [ViewModel] Factory for TaskViewModel — injects TodoRepository and ModuleRepository.
public class TaskViewModelFactory implements ViewModelProvider.Factory {

    private final TodoRepository todoRepository;
    private final ModuleRepository moduleRepository;

    public TaskViewModelFactory(TodoRepository todoRepository, ModuleRepository moduleRepository) {
        this.todoRepository = todoRepository;
        this.moduleRepository = moduleRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TaskViewModel.class)) {
            return (T) new TaskViewModel(todoRepository, moduleRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
