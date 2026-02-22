package com.k12.user.domain.model;

import static com.k12.user.domain.model.error.UserError.EmailError.EMAIL_INVALID_FORMAT;
import static com.k12.user.domain.model.error.UserError.NameError.NAME_EMPTY;
import static com.k12.user.domain.model.error.UserError.PasswordError.PASSWORD_HASH_INVALID;
import static com.k12.user.domain.model.error.UserError.RoleError.ROLES_CANNOT_BE_EMPTY;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.commands.UserCommands.CreateUser;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.model.events.UserEvents;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory for creating User instances and handling CreateUser commands.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserFactory {

    /**
     * Handles the CreateUser command and returns a Result with the created user.
     *
     * @param command The CreateUser command containing user data
     * @return Result containing UserCreated event on success, or UserError on failure
     */
    public static Result<UserEvents, UserError> handle(CreateUser command) {
        return create(command.email(), command.passwordHash(), command.roles(), command.name());
    }

    /**
     * Creates a new user with validation.
     *
     * @param email User's email address string
     * @param passwordHash User's password hash string
     * @param roles User's roles (must have at least one)
     * @param name User's name string
     * @return Result containing UserCreated event on success, or UserError on failure
     */
    public static Result<UserEvents, UserError> create(
            String email, String passwordHash, Set<UserRole> roles, String name) {

        // Validate and create EmailAddress
        EmailAddress emailAddress;
        try {
            emailAddress = EmailAddress.of(email);
        } catch (IllegalArgumentException e) {
            return Result.failure(EMAIL_INVALID_FORMAT);
        }

        // Validate and create PasswordHash
        PasswordHash hash;
        try {
            hash = PasswordHash.of(passwordHash);
        } catch (IllegalArgumentException e) {
            return Result.failure(PASSWORD_HASH_INVALID);
        }

        // Validate and create UserName
        UserName userName;
        try {
            userName = UserName.of(name);
        } catch (IllegalArgumentException e) {
            return Result.failure(NAME_EMPTY);
        }

        return create(emailAddress, hash, roles, userName);
    }

    /**
     * Creates a new user from already validated value objects.
     * Use this when you already have validated inputs.
     *
     * @param emailAddress Validated email address
     * @param passwordHash Validated password hash
     * @param roles User's roles (must have at least one)
     * @param name Validated user name
     * @return Result containing UserCreated event on success, or UserError on failure
     */
    public static Result<UserEvents, UserError> create(
            EmailAddress emailAddress, PasswordHash passwordHash, Set<UserRole> roles, UserName name) {

        if (roles == null || roles.isEmpty()) {
            return Result.failure(ROLES_CANNOT_BE_EMPTY);
        }

        UserId userId = UserId.generate();
        java.time.Instant now = java.time.Instant.now();
        long version = 1L;

        User user = new User(userId, emailAddress, passwordHash, roles, UserStatus.ACTIVE, name);

        return Result.success(new UserEvents.UserCreated(
                userId, emailAddress, passwordHash, roles, UserStatus.ACTIVE, name, now, version));
    }
}
