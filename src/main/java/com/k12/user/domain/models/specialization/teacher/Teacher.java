package com.k12.user.domain.model.specialization.teacher;

import java.time.Instant;

/**
 * Aggregate root representing a Teacher in the system.
 * TODO: Implement full teacher domain model with event sourcing
 */
public record Teacher(TeacherId teacherId, Instant createdAt) {

    public Teacher {
        if (teacherId == null) {
            throw new IllegalArgumentException("TeacherId cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
