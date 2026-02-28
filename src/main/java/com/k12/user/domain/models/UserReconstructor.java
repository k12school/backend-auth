package com.k12.user.domain.models;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.events.UserEvents;
import java.util.HashSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Reconstructs User state from a sequence of delta events.
 * Events contain only changes, not full state.
 * Reconstruction applies events incrementally.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserReconstructor {

    /**
     * Reconstructs a User from a list of delta events.
     * Events must be in chronological order and must start with UserCreated.
     *
     * @param events List of events in chronological order
     * @return Result containing the reconstructed User on success, or UserError on failure
     */
    public static Result<User, UserError> reconstruct(List<UserEvents> events) {
        if (events == null || events.isEmpty()) {
            return Result.failure(UserError.ValidationError.INVALID_VALUE);
        }

        User currentUser = null;
        UserId userId = null;

        for (UserEvents event : events) {
            // Validate event belongs to same aggregate
            if (userId != null) {
                UserId eventUserId = extractUserId(event);
                if (!eventUserId.equals(userId)) {
                    return Result.failure(UserError.ValidationError.INVALID_VALUE);
                }
            }

            currentUser = applyEvent(currentUser, event);

            // Track userId after first event
            if (userId == null && currentUser != null) {
                userId = currentUser.userId();
            }
        }

        if (currentUser == null) {
            return Result.failure(UserError.ValidationError.INVALID_VALUE);
        }

        return Result.success(currentUser);
    }

    /**
     * Reconstructs a User from events with full validation.
     * Validates:
     * - Events are in chronological order
     * - Versions are sequential
     * - First event is UserCreated
     * - All events for same aggregate
     *
     * @param events List of events in chronological order
     * @return Result containing the reconstructed User on success, or UserError on failure
     */
    public static Result<User, UserError> reconstructWithValidation(List<UserEvents> events) {
        if (events == null || events.isEmpty()) {
            return Result.failure(UserError.ValidationError.INVALID_VALUE);
        }

        // First event must be UserCreated
        if (!(events.getFirst() instanceof UserEvents.UserCreated)) {
            return Result.failure(UserError.ValidationError.VALUE_REQUIRED);
        }

        User currentUser = null;
        UserId userId = null;
        long expectedVersion = 1;

        for (UserEvents event : events) {
            // Validate version sequence
            Long eventVersion = extractVersion(event);
            if (eventVersion != null && eventVersion != expectedVersion) {
                return Result.failure(UserError.ValidationError.INVALID_VALUE);
            }

            // Validate same aggregate
            if (userId != null) {
                UserId eventUserId = extractUserId(event);
                if (!eventUserId.equals(userId)) {
                    return Result.failure(UserError.ValidationError.INVALID_VALUE);
                }
            }

            currentUser = applyEvent(currentUser, event);

            if (userId == null && currentUser != null) {
                userId = currentUser.userId();
            }

            expectedVersion++;
        }

        if (currentUser == null) {
            return Result.failure(UserError.ValidationError.INVALID_VALUE);
        }

        return Result.success(currentUser);
    }

    /**
     * Applies a single delta event to a User, returning the updated User.
     * Handles incremental state changes.
     *
     * @param user Current user state (null for UserCreated)
     * @param event Event to apply
     * @return Updated user with the event applied
     */
    public static User applyEvent(User user, UserEvents event) {
        return switch (event) {
            // UserCreated: Creates new user from initial state
            case UserEvents.UserCreated(
                    UserId userId,
                    var email,
                    var passwordHash,
                    var roles,
                    var status,
                    var name,
                    TenantId tenantId,
                    var createdAt,
                    var version) -> new User(userId, email, passwordHash, roles, status, name, tenantId);

            // UserSuspended: Changes status to SUSPENDED
            case UserEvents.UserSuspended(var userId, var suspendedAt, var version)
            when user != null -> user.withStatus(UserStatus.SUSPENDED);

            // UserActivated: Changes status to ACTIVE
            case UserEvents.UserActivated(var userId, var activatedAt, var version)
            when user != null -> user.withStatus(UserStatus.ACTIVE);

            // UserEmailUpdated: Changes email address
            case UserEvents.UserEmailUpdated(var userId, var newEmail, var previousEmail, var updatedAt, var version)
            when user != null -> user.withEmailAddress(newEmail);

            // UserPasswordUpdated: Changes password hash
            case UserEvents.UserPasswordUpdated(var userId, var changedAt, var version)
            when user != null ->
                // TODO
                // Password hash not stored in event for security
                // In real implementation, you'd fetch the new hash from a secure store
                // For now, return user unchanged
                user;

            // UserRoleAdded: Adds role to set
            case UserEvents.UserRoleAdded(var userId, var addedRole, var addedAt, var version)
            when user != null -> {
                var newRoles = new HashSet<>(user.userRole());
                newRoles.add(addedRole);
                yield user.withUserRole(newRoles);
            }

            // UserRoleRemoved: Removes role from set
            case UserEvents.UserRoleRemoved(var userId, var removedRole, var removedAt, var version)
            when user != null -> {
                var newRoles = new HashSet<>(user.userRole());
                newRoles.remove(removedRole);
                yield user.withUserRole(newRoles);
            }

            // UserNameUpdated: Changes name
            case UserEvents.UserNameUpdated(var userId, var newName, var previousName, var updatedAt, var version)
            when user != null -> user.withName(newName);

            // Handle null user case (shouldn't happen with proper event stream)
            default -> throw new IllegalStateException("Cannot apply event without existing user: " + event);
        };
    }

    /**
     * Extracts the UserId from an event.
     */
    private static UserId extractUserId(UserEvents event) {
        return switch (event) {
            case UserEvents.UserCreated(
                    var userId,
                    var email,
                    var passwordHash,
                    var roles,
                    var status,
                    var name,
                    var tenantId,
                    var createdAt,
                    var version) -> userId;
            case UserEvents.UserSuspended(var userId, var suspendedAt, var version) -> userId;
            case UserEvents.UserActivated(var userId, var activatedAt, var version) -> userId;
            case UserEvents.UserEmailUpdated(var userId, var newEmail, var previousEmail, var updatedAt, var version) ->
                userId;
            case UserEvents.UserPasswordUpdated(var userId, var changedAt, var version) -> userId;
            case UserEvents.UserRoleAdded(var userId, var addedRole, var addedAt, var version) -> userId;
            case UserEvents.UserRoleRemoved(var userId, var removedRole, var removedAt, var version) -> userId;
            case UserEvents.UserNameUpdated(var userId, var newName, var previousName, var updatedAt, var version) ->
                userId;
        };
    }

    /**
     * Extracts the version from an event (if present).
     */
    private static Long extractVersion(UserEvents event) {
        return switch (event) {
            case UserEvents.UserCreated(
                    var userId,
                    var email,
                    var passwordHash,
                    var roles,
                    var status,
                    var name,
                    var tenantId,
                    var createdAt,
                    long version) -> version;
            case UserEvents.UserSuspended(var userId, var suspendedAt, long version) -> version;
            case UserEvents.UserActivated(var userId, var activatedAt, long version) -> version;
            case UserEvents.UserEmailUpdated(
                    var userId,
                    var newEmail,
                    var previousEmail,
                    var updatedAt,
                    long version) -> version;
            case UserEvents.UserPasswordUpdated(var userId, var changedAt, long version) -> version;
            case UserEvents.UserRoleAdded(var userId, var addedRole, var addedAt, long version) -> version;
            case UserEvents.UserRoleRemoved(var userId, var removedRole, var removedAt, long version) -> version;
            case UserEvents.UserNameUpdated(var userId, var newName, var previousName, var updatedAt, long version) ->
                version;
        };
    }
}
