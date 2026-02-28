package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;
import java.time.Instant;

/**
 * Factory class for creating {@link Parent} aggregates.
 * <p>
 * Provides a single static method for creating Parent instances with validation
 * of required parameters. This factory ensures that all Parent aggregates are
 * created with valid state including a unique identifier and creation timestamp.
 * </p>
 */
public final class ParentFactory {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private ParentFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a new {@link Parent} aggregate with the specified parameters.
     * <p>
     * The created Parent will have its creation timestamp set to the current instant.
     * All fields except userId are optional and may be null.
     * </p>
     *
     * @param userId the unique user identifier for the parent; must not be null
     * @param phoneNumber the phone number (optional, may be null)
     * @param address the home address (optional, may be null)
     * @param emergencyContact the emergency contact information (optional, may be null)
     * @return a new Parent instance with the specified parameters and current timestamp
     * @throws IllegalArgumentException if userId is null
     */
    public static Parent create(UserId userId, String phoneNumber, String address, String emergencyContact) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        return new Parent(ParentId.of(userId), phoneNumber, address, emergencyContact, Instant.now());
    }
}
