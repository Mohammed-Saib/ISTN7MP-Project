package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.ModuleNoteRepository;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.NoteFolderRepository;
import com.example.mpproject.domain.repository.PersonalNoteAttachmentRepository;
import com.example.mpproject.domain.repository.PersonalNoteRepository;

public class NotesViewModelFactory implements ViewModelProvider.Factory {

    private final String userId;
    private final ModuleRepository moduleRepository;
    private final ModuleNoteRepository moduleNoteRepository;
    private final PersonalNoteRepository personalNoteRepository;
    private final PersonalNoteAttachmentRepository personalNoteAttachmentRepository;
    private final NoteFolderRepository noteFolderRepository;

    public NotesViewModelFactory(
            String userId,
            ModuleRepository moduleRepository,
            ModuleNoteRepository moduleNoteRepository,
            PersonalNoteRepository personalNoteRepository,
            PersonalNoteAttachmentRepository personalNoteAttachmentRepository,
            NoteFolderRepository noteFolderRepository
    ) {
        this.userId = userId;
        this.moduleRepository = moduleRepository;
        this.moduleNoteRepository = moduleNoteRepository;
        this.personalNoteRepository = personalNoteRepository;
        this.personalNoteAttachmentRepository = personalNoteAttachmentRepository;
        this.noteFolderRepository = noteFolderRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NotesViewModel.class)) {
            return (T) new NotesViewModel(
                    userId,
                    moduleRepository,
                    moduleNoteRepository,
                    personalNoteRepository,
                    personalNoteAttachmentRepository,
                    noteFolderRepository
            );
        }

        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}