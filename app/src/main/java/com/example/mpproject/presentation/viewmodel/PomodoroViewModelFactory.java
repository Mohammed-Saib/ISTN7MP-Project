package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.mpproject.domain.repository.PomodoroRepository;

public class PomodoroViewModelFactory implements ViewModelProvider.Factory {

    private final PomodoroRepository repository;

    public PomodoroViewModelFactory(PomodoroRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new PomodoroViewModel(repository);
    }
}