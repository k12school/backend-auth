package com.k12.domain.model.user.error;

/**
 * Base interface for all user-related errors.
 */
public sealed interface UserError
        permits UserError.UserStatusError,
                UserError.EmailError,
                UserError.PasswordError,
                UserError.RoleError,
                UserError.NameError,
                UserError.ValidationError {

    /**
     * Errors related to user status operations.
     */
    enum UserStatusError implements UserError {
        USER_ALREADY_SUSPENDED("User is already suspended"),
        USER_ALREADY_ACTIVE("User is already active");

        private final String message;

        UserStatusError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Errors related to email operations.
     */
    enum EmailError implements UserError {
        EMAIL_SAME_AS_CURRENT("New email is the same as current email"),
        EMAIL_EMPTY("Email cannot be null or empty"),
        EMAIL_INVALID_FORMAT("Invalid email format");

        private final String message;

        EmailError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Errors related to password operations.
     */
    enum PasswordError implements UserError {
        PASSWORD_SAME_AS_CURRENT("New password is the same as current password"),
        PASSWORD_HASH_INVALID("Invalid password hash format"),
        PASSWORD_HASH_EMPTY("Password hash cannot be empty"),
        PASSWORD_HASH_NULL("Password hash cannot be null");

        private final String message;

        PasswordError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Errors related to role operations.
     */
    enum RoleError implements UserError {
        ROLE_ALREADY_ASSIGNED("Role is already assigned to user"),
        ROLE_NOT_FOUND("Role not found in user roles"),
        CANNOT_REMOVE_LAST_ROLE("Cannot remove the last role from user"),
        ROLES_SAME_AS_CURRENT("New roles are the same as current roles"),
        ROLES_CANNOT_BE_EMPTY("User must have at least one role"),
        SUPER_ADMIN_ROLE_IMMUTABLE("Cannot modify SUPER_ADMIN role");

        private final String message;

        RoleError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Errors related to name operations.
     */
    enum NameError implements UserError {
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
     * General validation errors.
     */
    enum ValidationError implements UserError {
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
