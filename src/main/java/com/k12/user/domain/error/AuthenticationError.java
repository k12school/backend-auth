package com.k12.user.domain.error;

/**
 * Domain errors for authentication operations.
 * Following Result Either Pattern (ROP) for error handling.
 */
public sealed interface AuthenticationError {

    String message();

    /**
     * Invalid email or password combination.
     */
    record InvalidCredentials(String message) implements AuthenticationError {
        public InvalidCredentials() {
            this("Invalid email or password");
        }
    }

    /**
     * No account found with the provided email.
     */
    record UserNotFound(String message) implements AuthenticationError {
        public UserNotFound() {
            this("No account found with this email");
        }
    }

    /**
     * User account has been suspended.
     */
    record UserSuspended(String message) implements AuthenticationError {
        public UserSuspended() {
            this("Account has been suspended. Please contact support.");
        }
    }

    /**
     * User account is not active.
     */
    record UserInactive(String message) implements AuthenticationError {
        public UserInactive() {
            this("Account is not active. Please contact support.");
        }
    }
}
