package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.data.local.entity.UserEntity;

public interface AuthRepository {
    LiveData<UserEntity> register(String email, String password, String firstName, String lastName, String school);
    LiveData<UserEntity> login(String email, String password);
    void logout();
    UserEntity getCurrentUser();
    LiveData<String> getError();
    LiveData<Boolean> updateUserProfile(String firstName, String lastName, String email, String school);
}
