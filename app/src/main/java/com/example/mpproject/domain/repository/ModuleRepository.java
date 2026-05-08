package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.Module;

import java.util.List;

// [Domain] Contract for module operations — implemented in the data layer, consumed by the ViewModel.
public interface ModuleRepository {
    void insert(Module module);
    void update(Module module);
    void delete(Module module);

    LiveData<Module> getById(String moduleId);

    // All modules (active + archived) for a user
    LiveData<List<Module>> getAllByUser(String userId);

    // Only non-archived modules — used on the main module list screen
    LiveData<List<Module>> getActiveByUser(String userId);

    // Only archived modules — shown on the archived list
    LiveData<List<Module>> getArchivedByUser(String userId);
}
