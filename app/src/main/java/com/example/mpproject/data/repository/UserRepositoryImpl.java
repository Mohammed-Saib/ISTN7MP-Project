package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.UserDao;
import com.example.mpproject.data.local.entity.UserEntity;
import com.example.mpproject.domain.model.User;
import com.example.mpproject.domain.repository.UserRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// [Data] Implements UserRepository. Room is the source of truth; Firestore is synced in the background.
public class UserRepositoryImpl implements UserRepository {

    private static final String TAG = "UserRepository";

    private final UserDao userDao;
    private final FirebaseFirestore firestore;

    public UserRepositoryImpl(UserDao userDao) {
        this.userDao = userDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(User user) {
        // Write to Room first; Firestore sync is secondary
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.insert(toEntity(user));
            syncToFirestore(user);
        });
    }

    @Override
    public void update(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.update(toEntity(user));
            syncToFirestore(user);
        });
    }

    @Override
    public void delete(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.delete(toEntity(user)));
    }

    @Override
    public LiveData<User> getById(String userId) {
        return Transformations.map(userDao.getById(userId), this::toDomain);
    }

    @Override
    public LiveData<User> getCurrentUser() {
        return Transformations.map(userDao.getCurrentUser(), this::toDomain);
    }

    // Push the user profile to Firestore under users/{userId}
    private void syncToFirestore(User user) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("firstName", user.getFirstName());
            data.put("lastName", user.getLastName());
            data.put("email", user.getEmail());
            data.put("dateJoined", user.getDateJoined());

            firestore.collection("users").document(user.getUserId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
    }

    // Map entity → domain model
    private User toDomain(UserEntity e) {
        if (e == null) return null;
        User user = new User(e.getUserId(), e.getUsername(), e.getFirstName(),
                e.getLastName(), e.getEmail(), e.getDateJoined());
        user.setLastSyncedAt(e.getLastSyncedAt());
        return user;
    }

    // Map domain model → entity
    private UserEntity toEntity(User user) {
        UserEntity e = new UserEntity(user.getUserId(), user.getUsername(),
                user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getDateJoined());
        e.setLastSyncedAt(user.getLastSyncedAt());
        return e;
    }
}
