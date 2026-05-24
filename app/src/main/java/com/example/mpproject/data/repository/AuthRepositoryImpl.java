package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mpproject.domain.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// [Data] Implements AuthRepository using Firebase Auth and Firestore.
public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<String> errorLiveData;

    public AuthRepositoryImpl() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.userLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
        
        if (firebaseAuth.getCurrentUser() != null) {
            userLiveData.setValue(firebaseAuth.getCurrentUser());
        }
    }

    @Override
    public LiveData<FirebaseUser> register(String email, String password, String firstName, String lastName, String username, String school) {
        userLiveData.setValue(null);
        errorLiveData.setValue(null);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Auth user created successfully");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            String fullName = firstName + " " + lastName;
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            
                            user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    Log.d(TAG, "User profile updated in Auth");
                                } else {
                                    Log.e(TAG, "Profile update failed", profileTask.getException());
                                }

                                Map<String, Object> userData = new HashMap<>();
                                userData.put("firstName", firstName);
                                userData.put("lastName", lastName);
                                userData.put("username", username);
                                userData.put("email", email);
                                userData.put("school", school);
                                userData.put("dateJoined", System.currentTimeMillis());
                                
                                Log.d(TAG, "Attempting to write to Firestore...");
                                firestore.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnCompleteListener(firestoreTask -> {
                                            if (firestoreTask.isSuccessful()) {
                                                Log.d(TAG, "Firestore document created");
                                                userLiveData.setValue(user);
                                            } else {
                                                Log.e(TAG, "Firestore write failed", firestoreTask.getException());
                                                errorLiveData.setValue("Registration failed. Please try again.");
                                            }
                                        });
                            });
                        }
                    } else {
                        Log.e(TAG, "Auth creation failed", task.getException());
                        errorLiveData.setValue("Registration failed. Please check your details.");
                    }
                });
        return userLiveData;
    }

    @Override
    public LiveData<FirebaseUser> login(String email, String password) {
        userLiveData.setValue(null);
        errorLiveData.setValue(null);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userLiveData.setValue(firebaseAuth.getCurrentUser());
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            errorLiveData.setValue("No account found with this email.");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorLiveData.setValue("Incorrect email or password.");
                        } else {
                            Log.e(TAG, "Login failed", e);
                            errorLiveData.setValue("Login failed. Please try again.");
                        }
                    }
                });
        return userLiveData;
    }

    @Override
    public void logout() {
        firebaseAuth.signOut();
        userLiveData.setValue(null);
    }

    @Override
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    @Override
    public LiveData<String> getError() {
        return errorLiveData;
    }

    @Override
    public LiveData<Boolean> resetPassword(String email) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        success.setValue(true);
                    } else {
                        Log.e(TAG, "Password reset failed", task.getException());
                        errorLiveData.setValue("Failed to send reset email. Please try again.");
                        success.setValue(false);
                    }
                });
        return success;
    }

    @Override
    public LiveData<Boolean> updateUserProfile(String firstName, String lastName, String username, String school) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String fullName = firstName + " " + lastName;
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build();
            
            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("firstName", firstName);
                updates.put("lastName", lastName);
                updates.put("username", username);
                updates.put("school", school);

                firestore.collection("users").document(user.getUid())
                        .update(updates)
                        .addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                success.setValue(true);
                                userLiveData.setValue(user); // Trigger update
                            } else {
                                errorLiveData.setValue("Failed to update profile data");
                                success.setValue(false);
                            }
                        });
            });
        }
        return success;
    }

    @Override
    public LiveData<Map<String, Object>> getUserData(String uid) {
        MutableLiveData<Map<String, Object>> userData = new MutableLiveData<>();
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userData.setValue(documentSnapshot.getData());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserData failed", e);
                    errorLiveData.setValue("Failed to load profile data.");
                });
        return userData;
    }
}
