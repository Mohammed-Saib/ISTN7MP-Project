package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.ModuleNote;

import java.util.List;

// [Domain] Contract for module file note operations — implemented in the data layer, consumed by the ViewModel.
public interface ModuleNoteRepository {
    void insert(ModuleNote note);
    void update(ModuleNote note);
    void delete(ModuleNote note);

    LiveData<ModuleNote> getById(String noteId);

    // All uploaded files for a user across all modules, newest first
    LiveData<List<ModuleNote>> getAllByUser(String userId);

    // All uploaded files for a specific module, newest first
    LiveData<List<ModuleNote>> getAllByModule(String moduleId);
}
