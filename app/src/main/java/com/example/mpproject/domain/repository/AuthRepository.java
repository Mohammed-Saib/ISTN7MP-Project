package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.data.local.entity.UserEntity;
import java.util.Map;

public interface AuthRepository {
    LiveData<UserEntity> register(String email, String password, String firstName, String lastName, String username, String school);
    LiveData<UserEntity> login(String email, String password);
    void logout();
    UserEntity getCurrentUser();
    LiveData<String> getError();
    LiveData<Boolean> resetPassword(String email);
    LiveData<Boolean> updateUserProfile(String firstName, String lastName, String username, String school);
    LiveData<Map<String, Object>> getUserData(String uid);
}
