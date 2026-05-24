
package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.PersonalNote;

import java.util.List;

// [Domain] Contract for personal note operations — implemented in the data layer, consumed by the ViewModel.
public interface PersonalNoteRepository {
    void insert(PersonalNote note);
    void update(PersonalNote note);
    void delete(PersonalNote note);

    // Pull/sync personal notes from Firestore into Room cache
    void syncFromFirestore(String userId);

    LiveData<PersonalNote> getById(String noteId);

    // All notes for a user — pinned first, then most recently updated
    LiveData<List<PersonalNote>> getAllByUser(String userId);

    // Notes linked to a specific module, same sort order
    LiveData<List<PersonalNote>> getAllByModule(String moduleId);
}
