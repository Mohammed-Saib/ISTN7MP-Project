package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.PersonalNoteAttachmentDao;
import com.example.mpproject.data.local.entity.PersonalNoteAttachmentEntity;
import com.example.mpproject.domain.model.PersonalNoteAttachment;
import com.example.mpproject.domain.repository.PersonalNoteAttachmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PersonalNoteAttachmentRepositoryImpl implements PersonalNoteAttachmentRepository {

    private final PersonalNoteAttachmentDao attachmentDao;

    public PersonalNoteAttachmentRepositoryImpl(PersonalNoteAttachmentDao attachmentDao) {
        this.attachmentDao = attachmentDao;
    }

    @Override
    public void insert(PersonalNoteAttachment attachment, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                attachmentDao.insert(toEntity(attachment));
                if (callback != null) callback.accept(true);
            } catch (Exception e) {
                Log.e("PersonalNoteAttachRepo", "Insert failed", e);
                if (callback != null) callback.accept(false);
            }
        });
    }

    @Override
    public void update(PersonalNoteAttachment attachment, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                attachmentDao.insert(toEntity(attachment));
                if (callback != null) callback.accept(true);
            } catch (Exception e) {
                Log.e("PersonalNoteAttachRepo", "Update failed", e);
                if (callback != null) callback.accept(false);
            }
        });
    }

    @Override
    public void delete(PersonalNoteAttachment attachment) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                attachmentDao.deleteById(attachment.getAttachmentId());
            } catch (Exception e) {
                Log.e("PersonalNoteAttachRepo", "Delete failed", e);
            }
        });
    }

    @Override
    public void syncFromFirestore(String userId) {
        // No-op: local-only storage
    }

    @Override
    public LiveData<List<PersonalNoteAttachment>> getByNoteId(String noteId) {
        return Transformations.map(attachmentDao.getByNoteId(noteId), this::toDomainList);
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
            for (PersonalNoteAttachmentEntity e : entities) list.add(toDomain(e));
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