package com.example.mpproject.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.mpproject.domain.model.User;

// [Domain] Contract for user profile operations — implemented in the data layer, consumed by the ViewModel.
public interface UserRepository {
    void insert(User user);
    void update(User user);
    void delete(User user);

    // Get a specific user by their Firebase UID
    LiveData<User> getById(String userId);

    // Returns the single cached row (there is only ever one logged-in user at a time)
    LiveData<User> getCurrentUser();
}
