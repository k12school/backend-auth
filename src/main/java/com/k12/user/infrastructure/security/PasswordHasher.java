package com.k12.user.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.k12.user.domain.models.PasswordHash;

/**
 * Utility for hashing passwords using BCrypt.
 */
public final class PasswordHasher {
    private PasswordHasher() {}

    /**
     * Hash a plain text password using BCrypt.
     *
     * @param password the plain text password
     * @return the hashed password
     * @throws IllegalArgumentException if password is null or blank
     */
    public static PasswordHash hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        return PasswordHash.of(hash);
    }
}
