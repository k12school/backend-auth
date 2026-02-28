package com.k12.user.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for Teacher user details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherResponse(
        String userId,
        String email,
        String name,
        String role,
        String tenantId,
        String employeeId,
        String department,
        LocalDate hireDate,
        String status,
        Instant createdAt) {

    public static TeacherResponse from(
            com.k12.user.domain.models.specialization.teacher.Teacher teacher, com.k12.user.domain.models.User user) {
        return new TeacherResponse(
                user.userId().value().toString(),
                user.emailAddress().value(),
                user.name().value(),
                user.userRole().iterator().next().name(),
                user.tenantId().value(),
                teacher.employeeId(),
                teacher.department(),
                teacher.hireDate(),
                user.status().name(),
                teacher.createdAt());
    }
}
