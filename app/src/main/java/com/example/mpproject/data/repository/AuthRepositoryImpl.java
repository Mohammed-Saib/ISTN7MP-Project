package com.example.mpproject.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.LocalSessionManager;
import com.example.mpproject.data.local.dao.UserDao;
import com.example.mpproject.data.local.entity.UserEntity;
import com.example.mpproject.domain.repository.AuthRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepository";
    private final UserDao userDao;
    private final Context context;
    private final MutableLiveData<String> errorLiveData;

    public AuthRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(this.context);
        this.userDao = db.userDao();
        this.errorLiveData = new MutableLiveData<>();
    }

    @Override
    public LiveData<UserEntity> register(String email, String password, String firstName, String lastName, String username, String school) {
        MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
        errorLiveData.setValue(null);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity existing = userDao.getByEmailSync(email);
            if (existing != null) {
                errorLiveData.postValue("An account with this email already exists.");
                return;
            }

            String userId = UUID.randomUUID().toString();
            String hashedPassword = hashPassword(password);
            long now = System.currentTimeMillis();

            UserEntity user = new UserEntity(userId, username, firstName, lastName, email, hashedPassword, now);
            userDao.insert(user);

            LocalSessionManager.setCurrentUserId(context, userId);
            userLiveData.postValue(user);
        });

        return userLiveData;
    }

    @Override
    public LiveData<UserEntity> login(String email, String password) {
        MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
        errorLiveData.setValue(null);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity user = userDao.loginSync(email, hashPassword(password));
            if (user != null) {
                LocalSessionManager.setCurrentUserId(context, user.getUserId());
                userLiveData.postValue(user);
            } else {
                UserEntity existingByEmail = userDao.getByEmailSync(email);
                if (existingByEmail == null) {
                    errorLiveData.postValue("No account found with this email.");
                } else {
                    errorLiveData.postValue("Incorrect email or password.");
                }
            }
        });

        return userLiveData;
    }

    @Override
    public void logout() {
        LocalSessionManager.clearSession(context);
    }

    @Override
    public UserEntity getCurrentUser() {
        String userId = LocalSessionManager.getCurrentUserId(context);
        if (userId == null) return null;

        try {
            final UserEntity[] result = new UserEntity[1];
            final Object lock = new Object();

            AppDatabase.databaseWriteExecutor.execute(() -> {
                result[0] = userDao.getByEmailSync(null);
                synchronized (lock) { lock.notifyAll(); }
            });

            // For synchronous access, query directly on current thread
            // This is acceptable for session checks at app startup
            synchronized (lock) {
                try { lock.wait(2000); } catch (InterruptedException ignored) {}
            }

            return result[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public LiveData<String> getError() {
        return errorLiveData;
    }

    @Override
    public LiveData<Boolean> resetPassword(String email) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        errorLiveData.setValue(null);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity user = userDao.getByEmailSync(email);
            if (user == null) {
                errorLiveData.postValue("No account found with this email.");
                success.postValue(false);
            } else {
                success.postValue(true);
            }
        });

        return success;
    }

    @Override
    public LiveData<Boolean> updateUserProfile(String firstName, String lastName, String username, String school) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        String userId = LocalSessionManager.getCurrentUserId(context);

        if (userId == null) {
            errorLiveData.setValue("No user logged in.");
            success.setValue(false);
            return success;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.updateProfile(userId, firstName, lastName, username);
            success.postValue(true);
        });

        return success;
    }

    @Override
    public LiveData<Map<String, Object>> getUserData(String uid) {
        MutableLiveData<Map<String, Object>> userData = new MutableLiveData<>();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            LiveData<UserEntity> liveUser = userDao.getById(uid);
            // Use a synchronous approach for the one-shot read
            // We observe on a background thread and post result
            try {
                final UserEntity[] holder = new UserEntity[1];
                final Object lock = new Object();

                liveUser.observeForever(entity -> {
                    holder[0] = entity;
                    synchronized (lock) { lock.notifyAll(); }
                });

                synchronized (lock) { lock.wait(2000); }

                if (holder[0] != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("firstName", holder[0].getFirstName());
                    map.put("lastName", holder[0].getLastName());
                    map.put("username", holder[0].getUsername());
                    map.put("email", holder[0].getEmail());
                    map.put("dateJoined", holder[0].getDateJoined());
                    userData.postValue(map);
                }
            } catch (Exception e) {
                Log.e(TAG, "getUserData failed", e);
            }
        });

        return userData;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}
