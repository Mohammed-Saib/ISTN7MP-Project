package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.PersonalNoteDao;
import com.example.mpproject.data.local.entity.PersonalNoteEntity;
import com.example.mpproject.domain.model.PersonalNote;
import com.example.mpproject.domain.repository.PersonalNoteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PersonalNoteRepositoryImpl implements PersonalNoteRepository {

    private final PersonalNoteDao personalNoteDao;

    public PersonalNoteRepositoryImpl(PersonalNoteDao personalNoteDao) {
        this.personalNoteDao = personalNoteDao;
    }

    @Override
    public void insert(PersonalNote note, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                personalNoteDao.insert(toEntity(note));
                if (callback != null) callback.accept(true);
            } catch (Exception e) {
                Log.e("PersonalNoteRepo", "Insert failed", e);
                if (callback != null) callback.accept(false);
            }
        });
    }

    @Override
    public void update(PersonalNote note, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                personalNoteDao.update(toEntity(note));
                if (callback != null) callback.accept(true);
            } catch (Exception e) {
                Log.e("PersonalNoteRepo", "Update failed", e);
                if (callback != null) callback.accept(false);
            }
        });
    }

    @Override
    public void delete(PersonalNote note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                personalNoteDao.delete(toEntity(note));
            } catch (Exception e) {
                Log.e("PersonalNoteRepo", "Delete failed", e);
            }
        });
    }

    @Override
    public void syncFromFirestore(String userId) {
        // No-op: local-only storage
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
            for (PersonalNoteEntity e : entities) list.add(toDomain(e));
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