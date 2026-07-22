package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.UserEntity;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserEntity> getById(String userId);

    @Query("SELECT * FROM users LIMIT 1")
    LiveData<UserEntity> getCurrentUser();

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getByEmailSync(String email);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    UserEntity loginSync(String email, String password);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    LiveData<UserEntity> getByEmail(String email);

    @Query("UPDATE users SET firstName = :firstName, lastName = :lastName, username = :username WHERE userId = :userId")
    void updateProfile(String userId, String firstName, String lastName, String username);
}
