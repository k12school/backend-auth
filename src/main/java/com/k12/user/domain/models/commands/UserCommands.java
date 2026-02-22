package com.k12.user.domain.models.commands;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import java.util.Set;

/**
 * Sealed interface representing all possible user commands.
 * Each command is a record containing the data needed to execute it.
 */
public sealed interface UserCommands
        permits UserCommands.CreateUser,
                UserCommands.SuspendUser,
                UserCommands.ActivateUser,
                UserCommands.UpdateEmail,
                UserCommands.ChangePassword,
                UserCommands.AddRole,
                UserCommands.RemoveRole,
                UserCommands.UpdateName {

    /**
     * Command to create a new user.
     */
    record CreateUser(String email, String passwordHash, Set<UserRole> roles, String name) implements UserCommands {}

    /**
     * Command to suspend a user.
     */
    record SuspendUser(UserId userId) implements UserCommands {}

    /**
     * Command to activate a user.
     */
    record ActivateUser(UserId userId) implements UserCommands {}

    /**
     * Command to update a user's email address.
     */
    record UpdateEmail(UserId userId, EmailAddress newEmailAddress) implements UserCommands {}

    /**
     * Command to change a user's password.
     */
    record ChangePassword(UserId userId, PasswordHash newPasswordHash) implements UserCommands {}

    /**
     * Command to add a role to a user.
     */
    record AddRole(UserId userId, UserRole role) implements UserCommands {}

    /**
     * Command to remove a role from a user.
     */
    record RemoveRole(UserId userId, UserRole role) implements UserCommands {}

    /**
     * Command to update a user's name.
     */
    record UpdateName(UserId userId, UserName newName) implements UserCommands {}
}
