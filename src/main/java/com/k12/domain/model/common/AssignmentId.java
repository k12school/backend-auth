package com.k12.domain.model.common;

import java.util.UUID;

/**
 * Value object representing a unique identifier for an Assignment.
 */
public record AssignmentId(UUID value) {

    public static AssignmentId of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AssignmentId cannot be null");
        }
        try {
            return new AssignmentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + value, e);
        }
    }

    public static AssignmentId generate() {
        return new AssignmentId(UUID.randomUUID());
    }
}
