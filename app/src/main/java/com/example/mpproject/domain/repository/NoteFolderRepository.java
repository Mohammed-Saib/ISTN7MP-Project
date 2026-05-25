package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.NoteFolder;

import java.util.List;

public interface NoteFolderRepository {

    void insert(NoteFolder folder);

    void update(NoteFolder folder);

    void delete(NoteFolder folder);

    void syncFromFirestore(String userId);

    LiveData<NoteFolder> getById(String folderId);

    LiveData<List<NoteFolder>> getAllByUser(String userId);
}