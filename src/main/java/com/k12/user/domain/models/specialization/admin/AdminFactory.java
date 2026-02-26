package com.k12.user.domain.models.specialization.admin;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import java.time.Instant;
import java.util.Set;

/**
 * Factory class for creating {@link Admin} aggregates.
 * <p>
 * Provides a single static method for creating Admin instances with validation
 * of required parameters. This factory ensures that all Admin aggregates are
 * created with valid state including a unique identifier, permissions, and
 * creation timestamp.
 * </p>
 */
public final class AdminFactory {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private AdminFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a new {@link Admin} aggregate with the specified identifier and permissions.
     * <p>
     * The created Admin will have its creation timestamp set to the current instant.
     * </p>
     *
     * @param adminId the unique identifier for the admin; must not be null
     * @param permissions the set of permissions granted to the admin; must not be null or empty
     * @return a new Admin instance with the specified parameters and current timestamp
     * @throws IllegalArgumentException if adminId is null
     * @throws IllegalArgumentException if permissions is null or empty
     */
    public static Admin create(AdminId adminId, Set<Permission> permissions) {
        if (adminId == null) {
            throw new IllegalArgumentException("AdminId cannot be null");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("Permissions cannot be null or empty");
        }

        return new Admin(adminId, permissions, Instant.now());
    }
}
