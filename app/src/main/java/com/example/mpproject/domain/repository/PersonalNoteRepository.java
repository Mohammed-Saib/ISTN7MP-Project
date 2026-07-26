
package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.PersonalNote;

import java.util.List;
import java.util.function.Consumer;

public interface PersonalNoteRepository {
    void insert(PersonalNote note, Consumer<Boolean> callback);
    void update(PersonalNote note, Consumer<Boolean> callback);
    void delete(PersonalNote note);

    void syncFromFirestore(String userId);

    LiveData<PersonalNote> getById(String noteId);

    LiveData<List<PersonalNote>> getAllByUser(String userId);

    LiveData<List<PersonalNote>> getAllByModule(String moduleId);
}