package com.k12.tenant.domain.models;

/**
 * Value object representing a tenant's subdomain.
 */
public record Subdomain(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;
    private static final String VALID_SUBDOMAIN_PATTERN = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$";

    /**
     * Factory method to create a validated Subdomain.
     *
     * @param value The subdomain value
     * @return A validated Subdomain
     * @throws IllegalArgumentException if validation fails
     */
    public static Subdomain of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Subdomain cannot be null or empty");
        }
        String normalized = value.toLowerCase().trim();

        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Subdomain must be at least " + MIN_LENGTH + " characters long");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Subdomain cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!normalized.matches(VALID_SUBDOMAIN_PATTERN)) {
            throw new IllegalArgumentException(
                    "Subdomain contains invalid characters (only lowercase letters, numbers, and hyphens allowed)");
        }
        if (normalized.contains("--")) {
            throw new IllegalArgumentException("Subdomain cannot have consecutive hyphens");
        }
        if (normalized.startsWith("-") || normalized.endsWith("-")) {
            throw new IllegalArgumentException("Subdomain cannot start or end with a hyphen");
        }

        return new Subdomain(normalized);
    }
}
