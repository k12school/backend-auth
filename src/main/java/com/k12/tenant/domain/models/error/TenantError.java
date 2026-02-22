package com.k12.tenant.domain.models.error;

/**
 * Base interface for all tenant-related errors.
 */
public sealed interface TenantError
        permits TenantError.TenantStatusError,
                TenantError.NameError,
                TenantError.SubdomainError,
                TenantError.ValidationError {

    /**
     * Errors related to tenant status operations.
     */
    enum TenantStatusError implements TenantError {
        TENANT_ALREADY_SUSPENDED("Tenant is already suspended"),
        TENANT_ALREADY_ACTIVE("Tenant is already active"),
        TENANT_ALREADY_INACTIVE("Tenant is already inactive"),
        CANNOT_ACTIVATE_INACTIVE("Cannot activate a deactivated tenant");

        private final String message;

        TenantStatusError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Errors related to tenant name operations.
     */
    enum NameError implements TenantError {
        NAME_EMPTY("Name cannot be null or empty"),
        NAME_TOO_SHORT("Name must be at least 2 characters long"),
        NAME_TOO_LONG("Name cannot exceed 100 characters"),
        NAME_SAME_AS_CURRENT("New name is the same as current name"),
        NAME_CONTAINS_INVALID_CHARS("Name contains invalid characters");

        private final String message;

        NameError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Errors related to tenant subdomain operations.
     */
    enum SubdomainError implements TenantError {
        SUBDOMAIN_EMPTY("Subdomain cannot be null or empty"),
        SUBDOMAIN_TOO_SHORT("Subdomain must be at least 3 characters long"),
        SUBDOMAIN_TOO_LONG("Subdomain cannot exceed 63 characters"),
        SUBDOMAIN_SAME_AS_CURRENT("New subdomain is the same as current subdomain"),
        SUBDOMAIN_INVALID_FORMAT(
                "Subdomain contains invalid characters (only lowercase letters, numbers, and hyphens allowed)"),
        SUBDOMAIN_CANNOT_START_OR_END_WITH_HYPHEN("Subdomain cannot start or end with a hyphen"),
        SUBDOMAIN_HAS_CONSECUTIVE_HYPHENS("Subdomain cannot have consecutive hyphens");

        private final String message;

        SubdomainError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * General validation errors.
     */
    enum ValidationError implements TenantError {
        INVALID_VALUE("Invalid value provided"),
        VALUE_REQUIRED("Value is required");

        private final String message;

        ValidationError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }
}
