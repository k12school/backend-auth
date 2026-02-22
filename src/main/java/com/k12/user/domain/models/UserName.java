package com.k12.user.domain.model;

/**
 * User name value object.
 * Immutable, validated user name.
 */
public record UserName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    public UserName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        String normalized = value.trim();

        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Name must be at least " + MIN_LENGTH + " characters long");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Name cannot exceed " + MAX_LENGTH + " characters");
        }

        // Use normalized value
        value = normalized;
    }

    public static UserName of(String name) {
        return new UserName(name);
    }
}
