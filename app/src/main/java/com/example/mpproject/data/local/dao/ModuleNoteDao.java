package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.ModuleNoteEntity;

import java.util.List;

// [Data] Room DAO — all queries return LiveData so the View layer reacts automatically to changes.
@Dao
public interface ModuleNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ModuleNoteEntity note);

    @Update
    void update(ModuleNoteEntity note);

    @Delete
    void delete(ModuleNoteEntity note);

    // Look up a single file note by its UUID
    @Query("SELECT * FROM module_notes WHERE noteId = :noteId")
    LiveData<ModuleNoteEntity> getById(String noteId);

    // All uploaded files for a user across all modules, newest first
    @Query("SELECT * FROM module_notes WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<ModuleNoteEntity>> getAllByUser(String userId);

    // All uploaded files for a specific module, newest first
    @Query("SELECT * FROM module_notes WHERE moduleId = :moduleId ORDER BY createdAt DESC")
    LiveData<List<ModuleNoteEntity>> getAllByModule(String moduleId);
}
