package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.PersonalNoteAttachmentDao;
import com.example.mpproject.data.local.entity.PersonalNoteAttachmentEntity;
import com.example.mpproject.domain.model.PersonalNoteAttachment;
import com.example.mpproject.domain.repository.PersonalNoteAttachmentRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Attachment files are stored in Firebase Storage.
// Attachment metadata is cached in Room and synced to Firestore.
public class PersonalNoteAttachmentRepositoryImpl implements PersonalNoteAttachmentRepository {

    private static final String TAG = "PersonalAttachmentRepo";

    private final PersonalNoteAttachmentDao attachmentDao;
    private final FirebaseFirestore firestore;

    public PersonalNoteAttachmentRepositoryImpl(PersonalNoteAttachmentDao attachmentDao) {
        this.attachmentDao = attachmentDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(PersonalNoteAttachment attachment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            attachmentDao.insert(toEntity(attachment));
            syncToFirestore(attachment);
        });
    }

    @Override
    public void update(PersonalNoteAttachment attachment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            attachmentDao.insert(toEntity(attachment));
            syncToFirestore(attachment);
        });
    }

    @Override
    public void delete(PersonalNoteAttachment attachment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            attachmentDao.deleteById(attachment.getAttachmentId());

            firestore.collection("users").document(attachment.getUserId())
                    .collection("personal_notes").document(attachment.getNoteId())
                    .collection("attachments").document(attachment.getAttachmentId())
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore attachment delete failed", e));

            if (attachment.getStoragePath() != null && !attachment.getStoragePath().trim().isEmpty()) {
                FirebaseStorage.getInstance()
                        .getReference()
                        .child(attachment.getStoragePath())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Storage attachment delete failed", e));
            }
        });
    }

    @Override
    public void syncFromFirestore(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;

        firestore.collection("users").document(userId)
                .collection("personal_notes")
                .get()
                .addOnSuccessListener(noteSnap -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> attachmentDao.deleteAllByUser(userId));

                    for (DocumentSnapshot noteDoc : noteSnap.getDocuments()) {
                        String noteId = noteDoc.getId();

                        firestore.collection("users").document(userId)
                                .collection("personal_notes").document(noteId)
                                .collection("attachments")
                                .get()
                                .addOnSuccessListener(attachmentSnap ->
                                        AppDatabase.databaseWriteExecutor.execute(() -> {
                                            for (DocumentSnapshot doc : attachmentSnap.getDocuments()) {
                                                try {
                                                    PersonalNoteAttachmentEntity e = new PersonalNoteAttachmentEntity();

                                                    e.setAttachmentId(doc.getId());
                                                    e.setNoteId(noteId);
                                                    e.setUserId(userId);
                                                    e.setFileName(doc.getString("fileName"));
                                                    e.setFileType(doc.getString("fileType"));
                                                    e.setStoragePath(doc.getString("storagePath"));
                                                    e.setDownloadUrl(doc.getString("downloadUrl"));
                                                    e.setUploadStatus(doc.getString("uploadStatus"));

                                                    Long fileSizeBytes = doc.getLong("fileSizeBytes");
                                                    Long createdAt = doc.getLong("createdAt");

                                                    e.setFileSizeBytes(fileSizeBytes != null ? fileSizeBytes : 0L);
                                                    e.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());

                                                    attachmentDao.insert(e);
                                                } catch (Exception ex) {
                                                    Log.e(TAG, "Error syncing attachment from Firestore", ex);
                                                }
                                            }
                                        }))
                                .addOnFailureListener(e -> Log.e(TAG, "Attachment sync failed for note " + noteId, e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Personal note attachment parent sync failed", e));
    }

    @Override
    public LiveData<List<PersonalNoteAttachment>> getByNoteId(String noteId) {
        return Transformations.map(attachmentDao.getByNoteId(noteId), this::toDomainList);
    }

    private void syncToFirestore(PersonalNoteAttachment attachment) {
        Map<String, Object> data = new HashMap<>();

        data.put("attachmentId", attachment.getAttachmentId());
        data.put("noteId", attachment.getNoteId());
        data.put("userId", attachment.getUserId());
        data.put("fileName", attachment.getFileName());
        data.put("fileType", attachment.getFileType());
        data.put("fileSizeBytes", attachment.getFileSizeBytes());
        data.put("storagePath", attachment.getStoragePath());
        data.put("downloadUrl", attachment.getDownloadUrl());
        data.put("uploadStatus", attachment.getUploadStatus());
        data.put("createdAt", attachment.getCreatedAt());

        firestore.collection("users").document(attachment.getUserId())
                .collection("personal_notes").document(attachment.getNoteId())
                .collection("attachments").document(attachment.getAttachmentId())
                .set(data)
                .addOnFailureListener(e -> Log.e(TAG, "Firestore attachment sync failed", e));
    }

    private PersonalNoteAttachment toDomain(PersonalNoteAttachmentEntity e) {
        if (e == null) return null;

        PersonalNoteAttachment attachment = new PersonalNoteAttachment(
                e.getAttachmentId(),
                e.getNoteId(),
                e.getUserId(),
                e.getFileName(),
                e.getFileType()
        );

        attachment.setFileSizeBytes(e.getFileSizeBytes());
        attachment.setStoragePath(e.getStoragePath());
        attachment.setDownloadUrl(e.getDownloadUrl());
        attachment.setUploadStatus(e.getUploadStatus());
        attachment.setCreatedAt(e.getCreatedAt());

        return attachment;
    }

    private List<PersonalNoteAttachment> toDomainList(List<PersonalNoteAttachmentEntity> entities) {
        List<PersonalNoteAttachment> list = new ArrayList<>();

        if (entities != null) {
            for (PersonalNoteAttachmentEntity e : entities) {
                list.add(toDomain(e));
            }
        }

        return list;
    }

    private PersonalNoteAttachmentEntity toEntity(PersonalNoteAttachment attachment) {
        PersonalNoteAttachmentEntity e = new PersonalNoteAttachmentEntity();

        e.setAttachmentId(attachment.getAttachmentId());
        e.setNoteId(attachment.getNoteId());
        e.setUserId(attachment.getUserId());
        e.setFileName(attachment.getFileName());
        e.setFileType(attachment.getFileType());
        e.setFileSizeBytes(attachment.getFileSizeBytes());
        e.setStoragePath(attachment.getStoragePath());
        e.setDownloadUrl(attachment.getDownloadUrl());
        e.setUploadStatus(attachment.getUploadStatus());
        e.setCreatedAt(attachment.getCreatedAt());

        return e;
    }
}
