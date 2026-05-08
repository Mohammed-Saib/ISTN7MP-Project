package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.UserEntity;

// [Data] Room DAO — all queries return LiveData so the View layer reacts automatically to changes.
@Dao
public interface UserDao {

    // REPLACE handles the case where the user logs in again or a profile sync overwrites the row
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    // Look up the cached user by Firebase UID
    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserEntity> getById(String userId);

    // Convenience method — returns the one cached row (there is only ever one at a time)
    @Query("SELECT * FROM users LIMIT 1")
    LiveData<UserEntity> getCurrentUser();

    // Update only the sync timestamp so we know when we last pulled from Firestore
    @Query("UPDATE users SET lastSyncedAt = :timestamp WHERE userId = :userId")
    void updateLastSyncedAt(String userId, long timestamp);
}
