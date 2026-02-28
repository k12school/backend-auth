package com.k12.user.domain.models.specialization.student;

import com.k12.user.domain.models.specialization.parent.ParentId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Aggregate root representing a Student in the system.
 * <p>
 * A Student represents a user who is a student enrolled in the educational institution.
 * This aggregate contains student-specific information such as student ID, grade level,
 * date of birth, and optional guardian information.
 * </p>
 */
public record Student(
        StudentId studentId,
        String studentNumber,
        String gradeLevel,
        LocalDate dateOfBirth,
        ParentId guardianId,
        Instant createdAt) {

    /**
     * Creates a new Student instance with validation.
     * <p>
     * The studentId (StudentId value object) is required and must not be null.
     * The studentNumber is required and must not be null or blank.
     * The gradeLevel is required and must not be null or blank.
     * The dateOfBirth is required and must not be null or in the future.
     * The guardianId is optional and may be null (for orphan students).
     * The createdAt timestamp defaults to the current instant if not provided.
     * </p>
     *
     * @param studentId the unique identifier for this student; must not be null
     * @param studentNumber the student number/ID string; must not be null or blank
     * @param gradeLevel the grade level of the student; must not be null or blank
     * @param dateOfBirth the date of birth of the student; must not be null or in the future
     * @param guardianId the guardian/parent ID (optional, may be null)
     * @param createdAt the timestamp when this student was created; defaults to now if null
     * @throws IllegalArgumentException if studentId is null
     * @throws IllegalArgumentException if studentNumber is null or blank
     * @throws IllegalArgumentException if gradeLevel is null or blank
     * @throws IllegalArgumentException if dateOfBirth is null
     * @throws IllegalArgumentException if dateOfBirth is in the future
     */
    public Student {
        if (studentId == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalArgumentException("Student number cannot be null or blank");
        }
        if (gradeLevel == null || gradeLevel.isBlank()) {
            throw new IllegalArgumentException("Grade level cannot be null or blank");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        // guardianId is optional, can be null
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
