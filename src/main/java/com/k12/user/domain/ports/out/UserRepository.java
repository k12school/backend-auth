package com.k12.user.domain.ports.out;

import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import java.util.Optional;

/**
 * Repository interface for User aggregates.
 */
public interface UserRepository {

    /**
     * Saves a user aggregate.
     */
    User save(User user);

    /**
     * Finds a user by ID.
     */
    Optional<User> findById(UserId userId);

    /**
     * Finds a user by email address.
     */
    Optional<User> findByEmailAddress(String emailAddress);

    /**
     * Checks if a user exists by email.
     */
    boolean existsByEmail(String emailAddress);

    /**
     * Deletes a user by ID.
     */
    void deleteById(UserId userId);

    /**
     * Counts total users.
     */
    long count();

    /**
     * Finds a user by email within a specific tenant.
     *
     * @param email the email address
     * @param tenantId the tenant ID
     * @return the user if found
     */
    Optional<User> findByEmailInTenant(String email, TenantId tenantId);

    /**
     * Checks if a user exists by email within a specific tenant.
     *
     * @param email the email address
     * @param tenantId the tenant ID
     * @return true if exists, false otherwise
     */
    boolean existsByEmailInTenant(String email, TenantId tenantId);
}
