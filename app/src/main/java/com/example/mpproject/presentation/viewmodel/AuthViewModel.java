package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;
import java.util.Map;

// [ViewModel] Exposes auth operations to the View as LiveData; delegates all logic to AuthRepository.
public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;

    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<FirebaseUser> register(String email, String password, String firstName, String lastName, String username, String school) {
        return authRepository.register(email, password, firstName, lastName, username, school);
    }

    public LiveData<FirebaseUser> login(String email, String password) {
        return authRepository.login(email, password);
    }

    public void logout() {
        authRepository.logout();
    }

    public FirebaseUser getCurrentUser() {
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
