package com.k12.user.domain.ports.out;

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
}
