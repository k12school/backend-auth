package com.k12.tenant.domain.models.error;

/**
 * Base interface for all tenant admin creation errors.
 */
public sealed interface TenantAdminError
        permits TenantAdminError.ValidationError,
                TenantAdminError.ConflictError,
                TenantAdminError.PersistenceError {

    /**
     * Returns the error message.
     */
    String message();

    /**
     * Validation errors for tenant admin creation requests.
     */
    enum ValidationError implements TenantAdminError {
        INVALID_EMAIL("Invalid email format"),
        INVALID_PASSWORD("Password must be at least 8 characters long"),
        INVALID_NAME("Name cannot be null or empty"),
        INVALID_PERMISSIONS("At least one permission is required");

        private final String message;

        ValidationError(String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }

    /**
     * Conflict errors when resources already exist or don't exist.
     */
    enum ConflictError implements TenantAdminError {
        EMAIL_ALREADY_EXISTS("Email already exists"),
        TENANT_NOT_FOUND("Tenant not found");

        private final String message;

        ConflictError(String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }

    /**
     * Persistence layer errors during tenant admin creation.
     */
    enum PersistenceError implements TenantAdminError {
        USER_CREATION_FAILED("Failed to create user"),
        ADMIN_CREATION_FAILED("Failed to create admin aggregate");

        private final String message;

        PersistenceError(String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
