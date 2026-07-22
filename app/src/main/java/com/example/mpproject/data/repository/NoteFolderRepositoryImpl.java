package com.example.mpproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.NoteFolderDao;
import com.example.mpproject.data.local.entity.NoteFolderEntity;
import com.example.mpproject.domain.model.NoteFolder;
import com.example.mpproject.domain.repository.NoteFolderRepository;

import java.util.ArrayList;
import java.util.List;

public class NoteFolderRepositoryImpl implements NoteFolderRepository {

    private final NoteFolderDao noteFolderDao;

    public NoteFolderRepositoryImpl(NoteFolderDao noteFolderDao) {
        this.noteFolderDao = noteFolderDao;
    }

    @Override
    public void insert(NoteFolder folder) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteFolderDao.insert(toEntity(folder)));
    }

    @Override
    public void update(NoteFolder folder) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteFolderDao.update(toEntity(folder)));
    }

    @Override
    public void delete(NoteFolder folder) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteFolderDao.delete(toEntity(folder)));
    }

    @Override
    public void syncFromFirestore(String userId) {
        // No-op: local-only storage
    }

    @Override
    public LiveData<NoteFolder> getById(String folderId) {
        return Transformations.map(noteFolderDao.getById(folderId), this::toDomain);
    }

    @Override
    public LiveData<List<NoteFolder>> getAllByUser(String userId) {
        return Transformations.map(noteFolderDao.getAllByUser(userId), this::toDomainList);
    }

    private NoteFolder toDomain(NoteFolderEntity e) {
        if (e == null) return null;
        NoteFolder folder = new NoteFolder(e.getFolderId(), e.getUserId(), e.getName());
        folder.setCreatedAt(e.getCreatedAt());
        folder.setUpdatedAt(e.getUpdatedAt());
        return folder;
    }

    private List<NoteFolder> toDomainList(List<NoteFolderEntity> entities) {
        List<NoteFolder> list = new ArrayList<>();
        if (entities != null) {
            for (NoteFolderEntity e : entities) list.add(toDomain(e));
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
