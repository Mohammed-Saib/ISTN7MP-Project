package com.example.mpproject.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String school;
    private long dateJoined;
    private Long lastSyncedAt;

    public UserEntity(@NonNull String userId, String firstName,
                      String lastName, String email, String password, long dateJoined) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.dateJoined = dateJoined;
    }

    @NonNull public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public long getDateJoined() { return dateJoined; }
    public void setDateJoined(long dateJoined) { this.dateJoined = dateJoined; }
    public Long getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Long lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
