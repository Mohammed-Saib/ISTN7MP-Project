package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.ResearchPaperRepository;

public class ResearchViewModelFactory implements ViewModelProvider.Factory {

    private final ResearchPaperRepository repository;

    public ResearchViewModelFactory(ResearchPaperRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ResearchViewModel.class)) {
            return (T) new ResearchViewModel(repository);
        }

        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}