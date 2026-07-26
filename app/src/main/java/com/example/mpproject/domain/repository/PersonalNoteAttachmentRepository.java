package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.PersonalNoteAttachment;

import java.util.List;
import java.util.function.Consumer;

public interface PersonalNoteAttachmentRepository {

    void insert(PersonalNoteAttachment attachment, Consumer<Boolean> callback);

    void update(PersonalNoteAttachment attachment, Consumer<Boolean> callback);

    void delete(PersonalNoteAttachment attachment);

    void syncFromFirestore(String userId);

    LiveData<List<PersonalNoteAttachment>> getByNoteId(String noteId);
}