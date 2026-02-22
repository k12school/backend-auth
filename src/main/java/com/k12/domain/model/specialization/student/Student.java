package com.k12.domain.model.specialization.student;

import com.k12.domain.model.common.StudentId;
import java.time.Instant;

/**
 * Aggregate root representing a Student in the system.
 * TODO: Implement full student domain model with event sourcing
 */
public record Student(StudentId studentId, Instant createdAt) {

    public Student {
        if (studentId == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
