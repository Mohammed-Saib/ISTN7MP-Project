package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mpproject.data.local.entity.PersonalNoteAttachmentEntity;

import java.util.List;

@Dao
public interface PersonalNoteAttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PersonalNoteAttachmentEntity attachment);

    @Query("DELETE FROM personal_note_attachments WHERE attachmentId = :attachmentId")
    void deleteById(String attachmentId);

    @Query("DELETE FROM personal_note_attachments WHERE noteId = :noteId")
    void deleteByNoteId(String noteId);

    @Query("DELETE FROM personal_note_attachments WHERE userId = :userId")
    void deleteAllByUser(String userId);

    @Query("SELECT * FROM personal_note_attachments WHERE noteId = :noteId ORDER BY createdAt DESC")
    LiveData<List<PersonalNoteAttachmentEntity>> getByNoteId(String noteId);
}
