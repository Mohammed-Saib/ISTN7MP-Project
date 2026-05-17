package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.ModuleEntity;

import java.util.List;

// [Data] Room DAO — all queries return LiveData so the View layer reacts automatically to changes.
@Dao
public interface ModuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ModuleEntity module);

    @Update
    void update(ModuleEntity module);

    @Delete
    void delete(ModuleEntity module);

    // Look up a single module by its UUID
    @Query("SELECT * FROM modules WHERE moduleId = :moduleId")
    LiveData<ModuleEntity> getById(String moduleId);

    // All modules for a user, sorted alphabetically by name
    @Query("SELECT * FROM modules WHERE userId = :userId ORDER BY name ASC")
    LiveData<List<ModuleEntity>> getAllByUser(String userId);

    // Active (non-archived) modules only — used on the main module list screen
    @Query("SELECT * FROM modules WHERE userId = :userId AND isArchived = 0 ORDER BY name ASC")
    LiveData<List<ModuleEntity>> getActiveByUser(String userId);

    // Archived modules — shown on a separate archived list
    @Query("SELECT * FROM modules WHERE userId = :userId AND isArchived = 1 ORDER BY name ASC")
    LiveData<List<ModuleEntity>> getArchivedByUser(String userId);

    @Query("SELECT COUNT(*) FROM modules WHERE userId = :userId")
    int countByUser(String userId);
}
