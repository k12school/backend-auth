package com.k12.user.domain.models.specialization.teacher;

import com.k12.common.domain.model.UserId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Factory class for creating {@link Teacher} aggregates.
 * <p>
 * Provides a single static method for creating Teacher instances with validation
 * of required parameters. This factory ensures that all Teacher aggregates are
 * created with valid state including a unique identifier, employee ID, and
 * creation timestamp.
 * </p>
 */
public final class TeacherFactory {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private TeacherFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a new {@link Teacher} aggregate with the specified parameters.
     * <p>
     * The created Teacher will have its creation timestamp set to the current instant.
     * </p>
     *
     * @param userId the unique user identifier for the teacher; must not be null
     * @param employeeId the employee ID for the teacher; must not be null or blank
     * @param department the department the teacher belongs to; can be null
     * @param hireDate the hire date of the teacher; defaults to current date if null
     * @return a new Teacher instance with the specified parameters and current timestamp
     * @throws IllegalArgumentException if userId is null
     * @throws IllegalArgumentException if employeeId is null or blank
     */
    public static Teacher create(UserId userId, String employeeId, String department, LocalDate hireDate) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID cannot be null or blank");
        }

        return new Teacher(TeacherId.of(userId), employeeId, department, hireDate, Instant.now());
    }
}
