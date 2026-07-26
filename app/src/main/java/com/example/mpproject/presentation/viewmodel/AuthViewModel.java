package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.data.local.entity.UserEntity;
import com.example.mpproject.domain.repository.AuthRepository;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;

    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<UserEntity> register(String email, String password, String firstName, String lastName, String school) {
        return authRepository.register(email, password, firstName, lastName, school);
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

    public LiveData<Boolean> updateProfile(String firstName, String lastName, String email, String school) {
        return authRepository.updateUserProfile(firstName, lastName, email, school);
    }

    public LiveData<Boolean> changePassword(String currentPassword, String newPassword) {
        return authRepository.changePassword(currentPassword, newPassword);
    }
}
