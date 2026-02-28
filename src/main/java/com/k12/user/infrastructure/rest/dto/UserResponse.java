package com.k12.user.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Response DTO for user details with role-specific data.
 *
 * <p>This record provides a standardized response format for all user types, containing:
 * <ul>
 *   <li>Base user fields (userId, email, name, role, tenantId, status, createdAt)</li>
 *   <li>Role-specific nested data (TeacherData, ParentData, StudentData)</li>
 * </ul>
 *
 * <p>The nested records allow for polymorphic responses where only the relevant
 * role-specific data is populated based on the user's role. The @JsonInclude(NON_NULL)
 * annotation ensures that null nested records are omitted from JSON responses.
 */
@Schema(description = "Response payload containing user information with role-specific details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @Schema(description = "Email address of the user", example = "john.doe@example.com")
        String email,

        @Schema(description = "Full name of the user", example = "John Doe")
        String name,

        @Schema(description = "User role (TEACHER, PARENT, STUDENT, ADMIN)", example = "TEACHER")
        String role,

        @Schema(description = "Unique identifier of the tenant", example = "123e4567-e89b-12d3-a456-426614174000")
        String tenantId,

        @Schema(description = "Current status of the user (ACTIVE, INACTIVE, SUSPENDED)", example = "ACTIVE")
        String status,

        @Schema(description = "Timestamp when the user was created (ISO 8601 format)", example = "2024-01-15T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Teacher-specific data (present only if role is TEACHER)")
        TeacherData teacher,

        @Schema(description = "Parent-specific data (present only if role is PARENT)")
        ParentData parent,

        @Schema(description = "Student-specific data (present only if role is STUDENT)")
        StudentData student) {

    /**
     * Teacher-specific user data.
     */
    @Schema(description = "Teacher-specific attributes")
    public record TeacherData(
            @Schema(description = "Employee identification number", example = "EMP001")
            String employeeId,

            @Schema(description = "Department or faculty", example = "Mathematics")
            String department,

            @Schema(description = "Date when teacher was hired (ISO 8601 format)", example = "2020-08-15")
            String hireDate) {}

    /**
     * Parent-specific user data.
     */
    @Schema(description = "Parent-specific attributes")
    public record ParentData(
            @Schema(description = "Contact phone number", example = "+1-555-0123")
            String phoneNumber,

            @Schema(description = "Residential address", example = "123 Main St, City, State 12345")
            String address,

            @Schema(description = "Emergency contact information", example = "Jane Doe - +1-555-0987")
            String emergencyContact) {}

    /**
     * Student-specific user data.
     */
    @Schema(description = "Student-specific attributes")
    public record StudentData(
            @Schema(description = "Student identification number", example = "STU2024001")
            String studentNumber,

            @Schema(description = "Grade level or year", example = "10th Grade")
            String gradeLevel,

            @Schema(description = "Date of birth (ISO 8601 format)", example = "2010-05-15")
            String dateOfBirth,

            @Schema(description = "ID of the guardian parent", example = "123e4567-e89b-12d3-a456-426614174000")
            String guardianId) {}
}
