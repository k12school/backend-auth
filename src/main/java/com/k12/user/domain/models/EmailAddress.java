package com.k12.user.domain.models;

import java.util.regex.Pattern;

/**
 * Email address value object.
 * Immutable, validated email address.
 */
public record EmailAddress(String value) {

    // More restrictive pattern that rejects consecutive dots
    private static final Pattern VALID_EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9]+([._+-][A-Za-z0-9]+)*@[A-Za-z0-9]+([.-][A-Za-z0-9]+)*\\.[A-Za-z]{2,}$");

    public EmailAddress {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Email cannot be null or empty");
        }

        String normalized = value.trim().toLowerCase();

        if (!VALID_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("Invalid email format");
        }

        // Use normalized value
        value = normalized;
    }

    public static EmailAddress of(String email) {
        return new EmailAddress(email);
    }
}
