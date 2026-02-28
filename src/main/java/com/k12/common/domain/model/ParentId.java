package com.k12.common.domain.model;

/**
 * Value object representing a unique identifier for a Parent.
 * ParentId always contains the UserId - they are always the same value.
 */
public record ParentId(UserId userId) {

    public ParentId {
        if (userId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
    }

    public static ParentId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        return new ParentId(userId);
    }

    /**
     * Creates a ParentId from a UUID string.
     */
    public static ParentId fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        return new ParentId(UserId.of(value));
    }
}
