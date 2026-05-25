package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.NoteFolderEntity;

import java.util.List;

@Dao
public interface NoteFolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NoteFolderEntity folder);

    @Update
    void update(NoteFolderEntity folder);

    @Delete
    void delete(NoteFolderEntity folder);

    @Query("DELETE FROM note_folders WHERE userId = :userId")
    void deleteAllByUser(String userId);

    @Query("SELECT * FROM note_folders WHERE folderId = :folderId")
    LiveData<NoteFolderEntity> getById(String folderId);

    @Query("SELECT * FROM note_folders WHERE userId = :userId ORDER BY updatedAt DESC")
    LiveData<List<NoteFolderEntity>> getAllByUser(String userId);
}