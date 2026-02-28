package com.k12.user.domain.models.specialization.teacher;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Aggregate root representing a Teacher in the system.
 * TODO: Implement full teacher domain model with event sourcing
 */
public record Teacher(
        TeacherId teacherId, String employeeId, String department, LocalDate hireDate, Instant createdAt) {

    public Teacher {
        if (teacherId == null) {
            throw new IllegalArgumentException("TeacherId cannot be null");
        }
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID cannot be null or blank");
        }
        if (hireDate == null) {
            hireDate = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
