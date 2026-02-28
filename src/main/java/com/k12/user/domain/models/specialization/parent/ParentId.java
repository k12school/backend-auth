package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Parent.
 * <p>
 * ParentId wraps a UserId, providing type safety and domain-specific semantics.
 * The ParentId and UserId always represent the same underlying UUID value.
 * </p>
 */
public record ParentId(UserId value) {

    /**
     * Creates a new ParentId with the given UserId.
     *
     * @param value the UserId to wrap; must not be null
     * @throws IllegalArgumentException if value is null
     */
    public ParentId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    /**
     * Factory method to create a ParentId from a UserId.
     * <p>
     * This method provides a convenient way to create a ParentId with validation.
     * </p>
     *
     * @param userId the UserId to wrap; must not be null
     * @return a new ParentId instance
     * @throws IllegalArgumentException if userId is null
     */
    public static ParentId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        return new ParentId(userId);
    }

    /**
     * Returns the underlying UUID value.
     *
     * @return the UUID representing this ParentId
     */
    public UUID id() {
        return value.value();
    }
}
