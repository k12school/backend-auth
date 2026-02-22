package com.k12.domain.model.common;

import java.util.UUID;

/**
 * Value object representing a unique identifier for a Course.
 */
public record CourseId(UUID value) {

    public static CourseId of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("CourseId cannot be null");
        }
        try {
            return new CourseId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + value, e);
        }
    }

    public static CourseId generate() {
        return new CourseId(UUID.randomUUID());
    }
}
