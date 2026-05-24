package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.ModuleNoteDao;
import com.example.mpproject.data.local.entity.ModuleNoteEntity;
import com.example.mpproject.domain.model.ModuleNote;
import com.example.mpproject.domain.repository.ModuleNoteRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Implements ModuleNoteRepository. Room is the source of truth; Firestore is synced in the background.
public class ModuleNoteRepositoryImpl implements ModuleNoteRepository {

    private static final String TAG = "ModuleNoteRepository";

    private final ModuleNoteDao moduleNoteDao;
    private final FirebaseFirestore firestore;

    public ModuleNoteRepositoryImpl(ModuleNoteDao moduleNoteDao) {
        this.moduleNoteDao = moduleNoteDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(ModuleNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            moduleNoteDao.insert(toEntity(note));
            syncToFirestore(note);
        });
    }

    @Override
    public void update(ModuleNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            moduleNoteDao.update(toEntity(note));
            syncToFirestore(note);
        });
    }

    @Override
    public void delete(ModuleNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            moduleNoteDao.delete(toEntity(note));
            try {
                // Module notes are nested under modules in Firestore
                firestore.collection("users").document(note.getUserId())
                        .collection("modules").document(note.getModuleId())
                        .collection("module_notes").document(note.getNoteId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore delete error", e);
            }
        });
    }

    @Override
    public LiveData<ModuleNote> getById(String noteId) {
        return Transformations.map(moduleNoteDao.getById(noteId), this::toDomain);
    }

    @Override
    public LiveData<List<ModuleNote>> getAllByUser(String userId) {
        return Transformations.map(moduleNoteDao.getAllByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<ModuleNote>> getAllByModule(String moduleId) {
        return Transformations.map(moduleNoteDao.getAllByModule(moduleId), this::toDomainList);
    }

    // Unconditional merge-sync from Firestore.
    // Module notes are nested under modules, so this first fetches all module IDs for the user
    // then fetches each module's notes subcollection and upserts them into Room.
    public void syncFromFirestore(String userId) {
        firestore.collection("users").document(userId)
                .collection("modules")
                .get()
                .addOnSuccessListener(modulesSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot moduleDoc : modulesSnap.getDocuments()) {
                        String moduleId = moduleDoc.getId();
                        firestore.collection("users").document(userId)
                                .collection("modules").document(moduleId)
                                .collection("module_notes")
                                .get()
                                .addOnSuccessListener(notesSnap ->
                                        AppDatabase.databaseWriteExecutor.execute(() -> {
                                            for (com.google.firebase.firestore.DocumentSnapshot doc : notesSnap.getDocuments()) {
                                                try {
                                                    ModuleNoteEntity e = new ModuleNoteEntity();
                                                    e.setNoteId(doc.getId());
                                                    e.setModuleId(moduleId);
                                                    e.setUserId(doc.getString("userId") != null
                                                            ? doc.getString("userId") : userId);
                                                    e.setTitle(doc.getString("title"));
                                                    e.setFileName(doc.getString("fileName"));
                                                    e.setFileType(doc.getString("fileType"));
                                                    Long size = doc.getLong("fileSizeBytes");
                                                    e.setFileSizeBytes(size != null ? size : 0L);
                                                    e.setStorageUri(doc.getString("storageUri"));
                                                    e.setUploadStatus(doc.getString("uploadStatus"));
                                                    Long createdAt = doc.getLong("createdAt");
                                                    e.setCreatedAt(createdAt != null ? createdAt : 0L);
                                                    moduleNoteDao.insert(e);
                                                } catch (Exception ex) {
                                                    Log.e(TAG, "Error syncing module note from Firestore", ex);
                                                }
                                            }
                                        }))
                                .addOnFailureListener(e -> Log.e(TAG, "Firestore module_notes sync failed for module " + moduleId, e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore modules fetch failed during module note sync", e));
    }

    // Sync metadata to users/{userId}/modules/{moduleId}/module_notes/{noteId}
    private void syncToFirestore(ModuleNote note) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("title", note.getTitle());
            data.put("fileName", note.getFileName());
            data.put("fileType", note.getFileType());
            data.put("fileSizeBytes", note.getFileSizeBytes());
            data.put("storageUri", note.getStorageUri());
            data.put("uploadStatus", note.getUploadStatus());
            data.put("createdAt", note.getCreatedAt());
            data.put("userId", note.getUserId());

            firestore.collection("users").document(note.getUserId())
                    .collection("modules").document(note.getModuleId())
                    .collection("module_notes").document(note.getNoteId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
    }

    private ModuleNote toDomain(ModuleNoteEntity e) {
        if (e == null) return null;
        ModuleNote note = new ModuleNote(e.getNoteId(), e.getModuleId(), e.getUserId(),
                e.getTitle(), e.getFileName(), e.getFileType());
        note.setFileSizeBytes(e.getFileSizeBytes());
        note.setStorageUri(e.getStorageUri());
        note.setUploadStatus(e.getUploadStatus());
        return note;
    }

    private List<ModuleNote> toDomainList(List<ModuleNoteEntity> entities) {
        List<ModuleNote> list = new ArrayList<>();
        if (entities != null) {
            for (ModuleNoteEntity e : entities) list.add(toDomain(e));
        }
        return list;
    }

    private ModuleNoteEntity toEntity(ModuleNote note) {
        ModuleNoteEntity e = new ModuleNoteEntity();
        e.setNoteId(note.getNoteId());
        e.setModuleId(note.getModuleId());
        e.setUserId(note.getUserId());
        e.setTitle(note.getTitle());
        e.setFileName(note.getFileName());
        e.setFileType(note.getFileType());
        e.setFileSizeBytes(note.getFileSizeBytes());
        e.setStorageUri(note.getStorageUri());
        e.setUploadStatus(note.getUploadStatus());
        e.setCreatedAt(note.getCreatedAt());
        return e;
    }
}
