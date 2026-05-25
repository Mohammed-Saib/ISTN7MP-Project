package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.NoteFolderDao;
import com.example.mpproject.data.local.entity.NoteFolderEntity;
import com.example.mpproject.domain.model.NoteFolder;
import com.example.mpproject.domain.repository.NoteFolderRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Room is the local cache; Firestore syncs folders across devices.
public class NoteFolderRepositoryImpl implements NoteFolderRepository {

    private static final String TAG = "NoteFolderRepository";

    private final NoteFolderDao noteFolderDao;
    private final FirebaseFirestore firestore;

    public NoteFolderRepositoryImpl(NoteFolderDao noteFolderDao) {
        this.noteFolderDao = noteFolderDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(NoteFolder folder) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteFolderDao.insert(toEntity(folder));
            syncToFirestore(folder);
        });
    }

    @Override
    public void update(NoteFolder folder) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteFolderDao.update(toEntity(folder));
            syncToFirestore(folder);
        });
    }

    @Override
    public void delete(NoteFolder folder) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteFolderDao.delete(toEntity(folder));

            try {
                firestore.collection("users").document(folder.getUserId())
                        .collection("note_folders").document(folder.getFolderId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore folder delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore folder delete error", e);
            }
        });
    }

    @Override
    public void syncFromFirestore(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;

        firestore.collection("users").document(userId)
                .collection("note_folders")
                .get()
                .addOnSuccessListener(snap ->
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                noteFolderDao.deleteAllByUser(userId);

                                for (DocumentSnapshot doc : snap.getDocuments()) {
                                    NoteFolderEntity e = new NoteFolderEntity();

                                    e.setFolderId(doc.getId());
                                    e.setUserId(userId);
                                    e.setName(doc.getString("name"));

                                    Long createdAt = doc.getLong("createdAt");
                                    Long updatedAt = doc.getLong("updatedAt");

                                    long now = System.currentTimeMillis();

                                    e.setCreatedAt(createdAt != null ? createdAt : now);
                                    e.setUpdatedAt(updatedAt != null ? updatedAt : now);

                                    noteFolderDao.insert(e);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error syncing note folders from Firestore", ex);
                            }
                        }))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore note folders sync failed", e));
    }

    @Override
    public LiveData<NoteFolder> getById(String folderId) {
        return Transformations.map(noteFolderDao.getById(folderId), this::toDomain);
    }

    @Override
    public LiveData<List<NoteFolder>> getAllByUser(String userId) {
        return Transformations.map(noteFolderDao.getAllByUser(userId), this::toDomainList);
    }

    private void syncToFirestore(NoteFolder folder) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("folderId", folder.getFolderId());
            data.put("userId", folder.getUserId());
            data.put("name", folder.getName());
            data.put("createdAt", folder.getCreatedAt());
            data.put("updatedAt", folder.getUpdatedAt());

            firestore.collection("users").document(folder.getUserId())
                    .collection("note_folders").document(folder.getFolderId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore folder sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore folder sync error", e);
        }
    }

    private NoteFolder toDomain(NoteFolderEntity e) {
        if (e == null) return null;

        NoteFolder folder = new NoteFolder(
                e.getFolderId(),
                e.getUserId(),
                e.getName()
        );

        folder.setCreatedAt(e.getCreatedAt());
        folder.setUpdatedAt(e.getUpdatedAt());

        return folder;
    }

    private List<NoteFolder> toDomainList(List<NoteFolderEntity> entities) {
        List<NoteFolder> list = new ArrayList<>();

        if (entities != null) {
            for (NoteFolderEntity e : entities) {
                list.add(toDomain(e));
            }
        }

        return list;
    }

    private NoteFolderEntity toEntity(NoteFolder folder) {
        NoteFolderEntity e = new NoteFolderEntity();

        e.setFolderId(folder.getFolderId());
        e.setUserId(folder.getUserId());
        e.setName(folder.getName());
        e.setCreatedAt(folder.getCreatedAt());
        e.setUpdatedAt(folder.getUpdatedAt());

        return e;
    }
}