package com.k12.tenant.domain.models;

/**
 * Value object representing a tenant's name.
 */
public record TenantName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    /**
     * Factory method to create a validated TenantName.
     *
     * @param value The name value
     * @return A validated TenantName
     * @throws IllegalArgumentException if validation fails
     */
    public static TenantName of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Name must be at least " + MIN_LENGTH + " characters long");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Name cannot exceed " + MAX_LENGTH + " characters");
        }
        return new TenantName(trimmed);
    }
}
