package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.data.local.entity.UserEntity;
import com.example.mpproject.domain.repository.AuthRepository;
import java.util.Map;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;

    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<UserEntity> register(String email, String password, String firstName, String lastName, String username, String school) {
        return authRepository.register(email, password, firstName, lastName, username, school);
    }

    public LiveData<UserEntity> login(String email, String password) {
        return authRepository.login(email, password);
    }

    public void logout() {
        authRepository.logout();
    }

    public UserEntity getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public LiveData<String> getError() {
        return authRepository.getError();
    }

    public LiveData<Boolean> resetPassword(String email) {
        return authRepository.resetPassword(email);
    }

    public LiveData<Boolean> updateProfile(String firstName, String lastName, String username, String school) {
        return authRepository.updateUserProfile(firstName, lastName, username, school);
    }

    public LiveData<Map<String, Object>> getUserData(String uid) {
        return authRepository.getUserData(uid);
    }
}
