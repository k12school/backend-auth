package com.k12.user.domain.models;

import static com.k12.user.domain.models.error.UserError.EmailError.EMAIL_SAME_AS_CURRENT;
import static com.k12.user.domain.models.error.UserError.NameError.NAME_SAME_AS_CURRENT;
import static com.k12.user.domain.models.error.UserError.PasswordError.PASSWORD_SAME_AS_CURRENT;
import static com.k12.user.domain.models.error.UserError.RoleError.CANNOT_REMOVE_LAST_ROLE;
import static com.k12.user.domain.models.error.UserError.RoleError.ROLE_ALREADY_ASSIGNED;
import static com.k12.user.domain.models.error.UserError.RoleError.ROLE_NOT_FOUND;
import static com.k12.user.domain.models.error.UserError.UserStatusError.USER_ALREADY_ACTIVE;
import static com.k12.user.domain.models.error.UserError.UserStatusError.USER_ALREADY_SUSPENDED;
import static java.time.Instant.now;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.commands.UserCommands;
import com.k12.user.domain.models.commands.UserCommands.ActivateUser;
import com.k12.user.domain.models.commands.UserCommands.AddRole;
import com.k12.user.domain.models.commands.UserCommands.ChangePassword;
import com.k12.user.domain.models.commands.UserCommands.RemoveRole;
import com.k12.user.domain.models.commands.UserCommands.SuspendUser;
import com.k12.user.domain.models.commands.UserCommands.UpdateEmail;
import com.k12.user.domain.models.commands.UserCommands.UpdateName;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.events.UserEvents;
import com.k12.user.domain.models.events.UserEvents.UserActivated;
import com.k12.user.domain.models.events.UserEvents.UserEmailUpdated;
import com.k12.user.domain.models.events.UserEvents.UserNameUpdated;
import com.k12.user.domain.models.events.UserEvents.UserPasswordUpdated;
import com.k12.user.domain.models.events.UserEvents.UserRoleAdded;
import com.k12.user.domain.models.events.UserEvents.UserRoleRemoved;
import com.k12.user.domain.models.events.UserEvents.UserSuspended;
import java.util.Set;
import lombok.With;

public record User(
        UserId userId,
        @With EmailAddress emailAddress,
        @With PasswordHash passwordHash,
        @With Set<UserRole> userRole,
        @With UserStatus status,
        @With UserName name) {

    public Result<UserEvents, UserError> process(UserCommands command) {
        return switch (command) {
            case ActivateUser activateUser -> process(activateUser);
            case SuspendUser suspendUser -> process(suspendUser);
            case AddRole addRole -> process(addRole);
            case ChangePassword changePassword -> process(changePassword);
            case RemoveRole removeRole -> process(removeRole);
            case UpdateEmail updateEmail -> process(updateEmail);
            case UpdateName updateName -> process(updateName);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    /**
     * Suspends the user if not already suspended.
     * @return Result containing UserSuspended event on success, or UserStatusError on failure
     */
    private Result<UserEvents, UserError> process(SuspendUser command) {
        if (this.status == UserStatus.SUSPENDED) {
            return Result.failure(USER_ALREADY_SUSPENDED);
        }
        return Result.success(new UserSuspended(
                this.userId(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Activates the user if not already active.
     * @return Result containing UserActivated event on success, or UserStatusError on failure
     */
    private Result<UserEvents, UserError> process(ActivateUser command) {
        if (this.status == UserStatus.ACTIVE) {
            return Result.failure(USER_ALREADY_ACTIVE);
        }
        return Result.success(new UserActivated(
                this.userId(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Updates the user's email address with validation.
     * @return Result containing UserEmailUpdated event on success, or EmailError on failure
     */
    private Result<UserEvents, UserError> process(UpdateEmail command) {
        if (command.newEmailAddress().value().equals(this.emailAddress.value())) {
            return Result.failure(EMAIL_SAME_AS_CURRENT);
        }

        String previousEmail = this.emailAddress.value();
        return Result.success(new UserEmailUpdated(
                this.userId(),
                command.newEmailAddress(),
                previousEmail,
                now(),
                System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Changes the user's password hash.
     * @return Result containing UserPasswordUpdated event on success, or PasswordError on failure
     */
    private Result<UserEvents, UserError> process(ChangePassword command) {
        if (command.newPasswordHash().value().equals(this.passwordHash.value())) {
            return Result.failure(PASSWORD_SAME_AS_CURRENT);
        }

        return Result.success(new UserPasswordUpdated(
                this.userId(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Adds a role to the user if not already assigned.
     * @return Result containing UserRoleAdded event on success, or RoleError on failure
     */
    private Result<UserEvents, UserError> process(AddRole command) {
        if (this.userRole.contains(command.role())) {
            return Result.failure(ROLE_ALREADY_ASSIGNED);
        }

        return Result.success(new UserRoleAdded(this.userId(), command.role(), now(), System.currentTimeMillis()));
    }

    /**
     * Removes a role from the user if assigned.
     * Ensures user retains at least one role.
     * @return Result containing UserRoleRemoved event on success, or RoleError on failure
     */
    private Result<UserEvents, UserError> process(RemoveRole command) {
        if (!this.userRole.contains(command.role())) {
            return Result.failure(ROLE_NOT_FOUND);
        }

        if (this.userRole.size() == 1) {
            return Result.failure(CANNOT_REMOVE_LAST_ROLE);
        }

        return Result.success(new UserRoleRemoved(
                this.userId(), command.role(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Updates the user's name with validation.
     * @return Result containing UserNameUpdated event on success, or NameError on failure
     */
    private Result<UserEvents, UserError> process(UpdateName command) {
        if (command.newName().value().equals(this.name.value())) {
            return Result.failure(NAME_SAME_AS_CURRENT);
        }

        String previousName = this.name.value();
        return Result.success(new UserNameUpdated(
                this.userId(), command.newName(), previousName, now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Applies an event to this user, returning the updated user.
     * This is useful for event sourcing and replaying events.
     *
     * @param event The event to apply
     * @return The updated user with the event applied
     */
    public User apply(UserEvents event) {
        return UserReconstructor.applyEvent(this, event);
    }
}
