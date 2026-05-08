package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.AuthRepository;
import com.example.mpproject.domain.repository.TodoRepository;

// [ViewModel] Factory for manual DI — creates the correct ViewModel and injects its dependencies.
public class ViewModelFactory implements ViewModelProvider.Factory {

    // Nullable: auth-only screens (Login, Register, Profile) don't need a TodoRepository.
    @Nullable private final TodoRepository todoRepository;
    private final AuthRepository authRepository;

    public ViewModelFactory(@Nullable TodoRepository todoRepository, AuthRepository authRepository) {
        this.todoRepository = todoRepository;
        this.authRepository = authRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TaskViewModel.class)) {
            if (todoRepository == null) {
                throw new IllegalStateException("TodoRepository is required to create TaskViewModel");
            }
            return (T) new TaskViewModel(todoRepository);
        }
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(authRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
