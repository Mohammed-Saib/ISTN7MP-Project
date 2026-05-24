package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.PersonalNoteAttachment;

import java.util.List;

public interface PersonalNoteAttachmentRepository {

    void insert(PersonalNoteAttachment attachment);

    void update(PersonalNoteAttachment attachment);

    void delete(PersonalNoteAttachment attachment);

    void syncFromFirestore(String userId);

    LiveData<List<PersonalNoteAttachment>> getByNoteId(String noteId);
}
