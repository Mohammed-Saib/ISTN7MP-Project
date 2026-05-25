package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.PersonalNoteDao;
import com.example.mpproject.data.local.entity.PersonalNoteEntity;
import com.example.mpproject.domain.model.PersonalNote;
import com.example.mpproject.domain.repository.PersonalNoteRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Room is used as local cache; Firestore is the cloud source for cross-device sync.
public class PersonalNoteRepositoryImpl implements PersonalNoteRepository {

    private static final String TAG = "PersonalNoteRepository";

    private final PersonalNoteDao personalNoteDao;
    private final FirebaseFirestore firestore;

    public PersonalNoteRepositoryImpl(PersonalNoteDao personalNoteDao) {
        this.personalNoteDao = personalNoteDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(PersonalNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            personalNoteDao.insert(toEntity(note));
            syncToFirestore(note);
        });
    }

    @Override
    public void update(PersonalNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            personalNoteDao.update(toEntity(note));
            syncToFirestore(note);
        });
    }

    @Override
    public void delete(PersonalNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            personalNoteDao.delete(toEntity(note));

            try {
                firestore.collection("users").document(note.getUserId())
                        .collection("personal_notes").document(note.getNoteId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore delete error", e);
            }
        });
    }

    @Override
    public void syncFromFirestore(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;

        firestore.collection("users").document(userId)
                .collection("personal_notes")
                .get()
                .addOnSuccessListener(snap ->
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                personalNoteDao.deleteAllByUser(userId);

                                for (DocumentSnapshot doc : snap.getDocuments()) {
                                    PersonalNoteEntity e = new PersonalNoteEntity();

                                    e.setNoteId(doc.getId());
                                    e.setUserId(userId);
                                    e.setModuleId(doc.getString("moduleId"));
                                    e.setFolderId(doc.getString("folderId"));
                                    e.setTitle(doc.getString("title"));
                                    e.setContent(doc.getString("content"));
                                    e.setShareCode(doc.getString("shareCode"));

                                    Boolean pinned = doc.getBoolean("isPinned");
                                    e.setPinned(pinned != null && pinned);

                                    Boolean shared = doc.getBoolean("isShared");
                                    e.setShared(shared != null && shared);

                                    Long createdAt = doc.getLong("createdAt");
                                    Long updatedAt = doc.getLong("updatedAt");

                                    long now = System.currentTimeMillis();

                                    e.setCreatedAt(createdAt != null ? createdAt : now);
                                    e.setUpdatedAt(updatedAt != null ? updatedAt : now);

                                    personalNoteDao.insert(e);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error syncing personal notes from Firestore", ex);
                            }
                        }))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore personal notes sync failed", e));
    }

    @Override
    public LiveData<PersonalNote> getById(String noteId) {
        return Transformations.map(personalNoteDao.getById(noteId), this::toDomain);
    }

    @Override
    public LiveData<List<PersonalNote>> getAllByUser(String userId) {
        return Transformations.map(personalNoteDao.getAllByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<PersonalNote>> getAllByModule(String moduleId) {
        return Transformations.map(personalNoteDao.getAllByModule(moduleId), this::toDomainList);
    }

    // Sync to users/{userId}/personal_notes/{noteId} in Firestore
    private void syncToFirestore(PersonalNote note) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", note.getUserId());
            data.put("title", note.getTitle());
            data.put("content", note.getContent());
            data.put("moduleId", note.getModuleId());
            data.put("folderId", note.getFolderId());
            data.put("isPinned", note.isPinned());
            data.put("isShared", note.isShared());
            data.put("shareCode", note.getShareCode());
            data.put("createdAt", note.getCreatedAt());
            data.put("updatedAt", note.getUpdatedAt());

            firestore.collection("users").document(note.getUserId())
                    .collection("personal_notes").document(note.getNoteId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
    }

    private PersonalNote toDomain(PersonalNoteEntity e) {
        if (e == null) return null;

        PersonalNote note = new PersonalNote(e.getNoteId(), e.getUserId(), e.getTitle());
        note.setModuleId(e.getModuleId());
        note.setFolderId(e.getFolderId());
        note.setContent(e.getContent());
        note.setShareCode(e.getShareCode());
        note.setPinned(e.isPinned());
        note.setShared(e.isShared());
        note.setCreatedAt(e.getCreatedAt());
        note.setUpdatedAt(e.getUpdatedAt());

        return note;
    }

    private List<PersonalNote> toDomainList(List<PersonalNoteEntity> entities) {
        List<PersonalNote> list = new ArrayList<>();

        if (entities != null) {
            for (PersonalNoteEntity e : entities) {
                list.add(toDomain(e));
            }
        }

        return list;
    }

    private PersonalNoteEntity toEntity(PersonalNote note) {
        PersonalNoteEntity e = new PersonalNoteEntity();

        e.setNoteId(note.getNoteId());
        e.setUserId(note.getUserId());
        e.setModuleId(note.getModuleId());
        e.setFolderId(note.getFolderId());
        e.setTitle(note.getTitle());
        e.setContent(note.getContent());
        e.setShareCode(note.getShareCode());
        e.setPinned(note.isPinned());
        e.setShared(note.isShared());
        e.setCreatedAt(note.getCreatedAt());
        e.setUpdatedAt(note.getUpdatedAt());

        return e;
    }
}