package com.k12.user.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for Student user details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentResponse(
        String userId,
        String email,
        String name,
        String role,
        String tenantId,
        String studentNumber,
        String gradeLevel,
        LocalDate dateOfBirth,
        String guardianId,
        String status,
        Instant createdAt) {

    public static StudentResponse from(
            com.k12.user.domain.models.specialization.student.Student student, com.k12.user.domain.models.User user) {
        String guardianIdValue = student.guardianId() != null
                ? student.guardianId().value().value().toString()
                : null;

        return new StudentResponse(
                user.userId().value().toString(),
                user.emailAddress().value(),
                user.name().value(),
                user.userRole().iterator().next().name(),
                user.tenantId().value(),
                student.studentNumber(),
                student.gradeLevel(),
                student.dateOfBirth(),
                guardianIdValue,
                user.status().name(),
                student.createdAt());
    }
}
