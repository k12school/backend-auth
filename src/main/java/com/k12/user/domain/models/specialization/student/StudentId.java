package com.k12.user.domain.models.specialization.student;

import com.k12.common.domain.model.UserId;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Student.
 * <p>
 * StudentId wraps a UserId, providing type safety and domain-specific semantics.
 * The StudentId and UserId always represent the same underlying UUID value.
 * </p>
 */
public record StudentId(UserId value) {

    /**
     * Creates a new StudentId with the given UserId.
     *
     * @param value the UserId to wrap; must not be null
     * @throws IllegalArgumentException if value is null
     */
    public StudentId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    /**
     * Factory method to create a StudentId from a UserId.
     * <p>
     * This method provides a convenient way to create a StudentId with validation.
     * </p>
     *
     * @param userId the UserId to wrap; must not be null
     * @return a new StudentId instance
     * @throws IllegalArgumentException if userId is null
     */
    public static StudentId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        return new StudentId(userId);
    }

    /**
     * Returns the underlying UUID value.
     *
     * @return the UUID representing this StudentId
     */
    public UUID id() {
        return value.value();
    }
}
