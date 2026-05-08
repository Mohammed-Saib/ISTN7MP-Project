package com.example.mpproject.domain.model;

// [Domain] Plain Java — no Room or Firebase annotations. Used by the ViewModel and Repository layers.
public class User {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private long dateJoined;
    private Long lastSyncedAt; // null until the first Firestore sync completes

    public User(String userId, String username, String firstName, String lastName,
                String email, long dateJoined) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dateJoined = dateJoined;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public long getDateJoined() { return dateJoined; }
    public Long getLastSyncedAt() { return lastSyncedAt; }

    public void setLastSyncedAt(Long lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
