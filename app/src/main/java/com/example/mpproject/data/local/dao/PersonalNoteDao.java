package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.PersonalNoteEntity;

import java.util.List;

// [Data] Room DAO — all queries return LiveData so the View layer reacts automatically to changes.
@Dao
public interface PersonalNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PersonalNoteEntity note);

    @Update
    void update(PersonalNoteEntity note);

    @Delete
    void delete(PersonalNoteEntity note);

    @Query("DELETE FROM personal_notes WHERE userId = :userId")
    void deleteAllByUser(String userId);

    // Look up a single note by its UUID
    @Query("SELECT * FROM personal_notes WHERE noteId = :noteId")
    LiveData<PersonalNoteEntity> getById(String noteId);

    // All notes for a user — pinned first, then sorted by most recently updated
    @Query("SELECT * FROM personal_notes WHERE userId = :userId ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<PersonalNoteEntity>> getAllByUser(String userId);

    // Notes linked to a specific module — same sort order as the main list
    @Query("SELECT * FROM personal_notes WHERE moduleId = :moduleId ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<PersonalNoteEntity>> getAllByModule(String moduleId);
}
