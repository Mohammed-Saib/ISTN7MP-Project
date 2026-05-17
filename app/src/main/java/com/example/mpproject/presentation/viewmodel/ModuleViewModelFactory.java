package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.ModuleRepository;

// [ViewModel] Factory for ModuleViewModel — injects ModuleRepository and userId.
public class ModuleViewModelFactory implements ViewModelProvider.Factory {

    private final ModuleRepository moduleRepository;
    private final String userId;

    public ModuleViewModelFactory(ModuleRepository moduleRepository, String userId) {
        this.moduleRepository = moduleRepository;
        this.userId = userId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ModuleViewModel.class)) {
            return (T) new ModuleViewModel(moduleRepository, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
