package com.example.mpproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.ModuleNoteDao;
import com.example.mpproject.data.local.entity.ModuleNoteEntity;
import com.example.mpproject.domain.model.ModuleNote;
import com.example.mpproject.domain.repository.ModuleNoteRepository;

import java.util.ArrayList;
import java.util.List;

public class ModuleNoteRepositoryImpl implements ModuleNoteRepository {

    private final ModuleNoteDao moduleNoteDao;

    public ModuleNoteRepositoryImpl(ModuleNoteDao moduleNoteDao) {
        this.moduleNoteDao = moduleNoteDao;
    }

    @Override
    public void insert(ModuleNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> moduleNoteDao.insert(toEntity(note)));
    }

    @Override
    public void update(ModuleNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> moduleNoteDao.update(toEntity(note)));
    }

    @Override
    public void delete(ModuleNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> moduleNoteDao.delete(toEntity(note)));
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

    private ModuleNote toDomain(ModuleNoteEntity e) {
        if (e == null) return null;
        ModuleNote note = new ModuleNote(
                e.getNoteId(),
                e.getModuleId(),
                e.getUserId(),
                e.getTitle(),
                e.getFileName(),
                e.getFileType()
        );
        note.setFolderId(e.getFolderId());
        note.setFileSizeBytes(e.getFileSizeBytes());
        note.setStorageUri(e.getStorageUri());
        note.setUploadStatus(e.getUploadStatus());
        note.setCreatedAt(e.getCreatedAt());
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
        e.setFolderId(note.getFolderId());
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
