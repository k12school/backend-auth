package com.k12.user.domain.models.events;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Domain events representing state changes in the User aggregate.
 * Events contain ONLY the changed data (deltas), not full state.
 * Reconstruction applies events incrementally to build current state.
 */
public sealed interface UserEvents
        permits UserEvents.UserCreated,
                UserEvents.UserSuspended,
                UserEvents.UserActivated,
                UserEvents.UserEmailUpdated,
                UserEvents.UserPasswordUpdated,
                UserEvents.UserRoleAdded,
                UserEvents.UserRoleRemoved,
                UserEvents.UserNameUpdated {

    /**
     * User was created - contains initial state (all fields).
     * This is the only event that contains full user data.
     */
    record UserCreated(
            UserId userId,
            EmailAddress email,
            PasswordHash passwordHash,
            Set<UserRole> roles,
            UserStatus status,
            UserName name,
            Instant createdAt,
            long version)
            implements UserEvents {}

    /**
     * User was suspended - only needs userId.
     */
    record UserSuspended(UserId userId, Instant suspendedAt, long version) implements UserEvents {}

    /**
     * User was activated - only needs userId.
     */
    record UserActivated(UserId userId, Instant activatedAt, long version) implements UserEvents {}

    /**
     * User email was updated - contains both old and new email.
     */
    record UserEmailUpdated(UserId userId, EmailAddress newEmail, String previousEmail, Instant updatedAt, long version)
            implements UserEvents {}

    /**
     * User password was changed - only needs userId (hash not stored in event for security).
     */
    record UserPasswordUpdated(UserId userId, Instant changedAt, long version) implements UserEvents {}

    /**
     * Role was added to user - contains the added role.
     */
    record UserRoleAdded(UserId userId, UserRole addedRole, Instant addedAt, long version) implements UserEvents {}

    /**
     * Role was removed from user - contains the removed role.
     */
    record UserRoleRemoved(UserId userId, UserRole removedRole, Instant removedAt, long version)
            implements UserEvents {}

    /**
     * User name was updated - contains both old and new name.
     */
    record UserNameUpdated(UserId userId, UserName newName, String previousName, Instant updatedAt, long version)
            implements UserEvents {}
}
