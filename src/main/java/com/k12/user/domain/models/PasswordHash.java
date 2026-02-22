package com.k12.user.domain.models;

/**
 * Password hash value object.
 * Immutable BCrypt hash.
 */
public record PasswordHash(String value) {

    private static final String BCRYPT_PATTERN = "^\\$2[aby]\\$\\d{2}\\$.{53,}$";

    public PasswordHash {
        if (value == null) {
            throw new IllegalArgumentException("Password hash cannot be null");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        if (!value.matches(BCRYPT_PATTERN)) {
            throw new IllegalArgumentException("Invalid BCrypt hash format: " + value);
        }
    }

    public static PasswordHash of(String hash) {
        return new PasswordHash(hash);
    }
}
