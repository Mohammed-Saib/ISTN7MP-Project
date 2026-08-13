package com.example.mpproject.data.local;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Single place that defines how passwords are hashed before they are written to the users table.
// Shared by AuthRepositoryImpl (register / login / change password) and DemoDataSeeder, so the
// seeded demo account hashes identically to one created through the register screen.
public final class PasswordHasher {

    private PasswordHasher() {}

    public static String sha256(String password) {
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
