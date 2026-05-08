package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;
import com.google.firebase.auth.FirebaseUser;
import java.util.Map;

// [Domain] Contract for authentication operations — implemented in the data layer, consumed by the ViewModel.
public interface AuthRepository {
    LiveData<FirebaseUser> register(String email, String password, String firstName, String lastName, String username, String school);
    LiveData<FirebaseUser> login(String email, String password);
    void logout();
    FirebaseUser getCurrentUser();
    LiveData<String> getError();
    LiveData<Boolean> resetPassword(String email);
    LiveData<Boolean> updateUserProfile(String firstName, String lastName, String username, String school);
    LiveData<Map<String, Object>> getUserData(String uid);
}
