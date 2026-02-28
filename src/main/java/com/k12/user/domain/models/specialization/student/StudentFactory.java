package com.k12.user.domain.models.specialization.student;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.ParentId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Factory class for creating {@link Student} aggregates.
 * <p>
 * Provides a single static method for creating Student instances with validation
 * of required parameters. This factory ensures that all Student aggregates are
 * created with valid state including a unique identifier and creation timestamp.
 * </p>
 */
public final class StudentFactory {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private StudentFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a new {@link Student} aggregate with the specified parameters.
     * <p>
     * The created Student will have its creation timestamp set to the current instant.
     * The guardianId field is optional and may be null (for orphan students).
     * All other fields are required.
     * </p>
     *
     * @param userId the unique user identifier for the student; must not be null
     * @param studentNumber the student number/ID string; must not be null or blank
     * @param gradeLevel the grade level of the student; must not be null or blank
     * @param dateOfBirth the date of birth of the student; must not be null or in the future
     * @param guardianId the guardian/parent ID (optional, may be null)
     * @return a new Student instance with the specified parameters and current timestamp
     * @throws IllegalArgumentException if userId is null
     * @throws IllegalArgumentException if studentNumber is null or blank
     * @throws IllegalArgumentException if gradeLevel is null or blank
     * @throws IllegalArgumentException if dateOfBirth is null
     * @throws IllegalArgumentException if dateOfBirth is in the future
     */
    public static Student create(
            UserId userId, String studentNumber, String gradeLevel, LocalDate dateOfBirth, ParentId guardianId) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        return new Student(StudentId.of(userId), studentNumber, gradeLevel, dateOfBirth, guardianId, Instant.now());
    }
}
