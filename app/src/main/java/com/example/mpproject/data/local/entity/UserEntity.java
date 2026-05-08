package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Local single-row cache of the authenticated user's profile.
// Written after login; updated on profile edit.
@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String userId;      // Firebase Auth UID (primary key)
    private String username;    // display name chosen by the user
    private String firstName;
    private String lastName;
    private String email;       // from Firebase Auth, not directly editable
    private long dateJoined;    // unix ms — set on register, never updated
    private Long lastSyncedAt;  // unix ms of last Firestore sync; local-only, null = never synced

    public UserEntity(@NonNull String userId, String username, String firstName,
                      String lastName, String email, long dateJoined) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dateJoined = dateJoined;
    }

    @NonNull public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public long getDateJoined() { return dateJoined; }
    public void setDateJoined(long dateJoined) { this.dateJoined = dateJoined; }
    public Long getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Long lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
