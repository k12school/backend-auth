package com.k12.domain.model.common;

/**
 * Value object representing a unique identifier for a Student.
 * StudentId always contains the UserId - they are always the same value.
 */
public record StudentId(UserId userId) {

    public StudentId {
        if (userId == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
    }

    public static StudentId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
        return new StudentId(userId);
    }

    /**
     * Creates a StudentId from a UUID string.
     */
    public static StudentId fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
        return new StudentId(UserId.of(value));
    }
}
