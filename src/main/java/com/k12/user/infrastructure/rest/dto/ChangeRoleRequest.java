package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request DTO for changing a user's role with new specialization data.
 *
 * <p>This record is used for PUT /api/users/{id}/role endpoint to change a user's role
 * and provide the necessary role-specific data for the new role. This is different from
 * {@link UpdateUserRequest} which is used for partial field updates within the same role.
 *
 * <p>Validation behavior:
 * <ul>
 *   <li><b>newRole:</b> Required field - must specify the target role</li>
 *   <li><b>Role-specific fields:</b> Validated at the service layer based on the target role</li>
 *   <li>Only provide fields applicable to the target role (other fields are ignored)</li>
 * </ul>
 *
 * <p>Role-specific field requirements by target role:
 * <ul>
 *   <li><b>TEACHER:</b> employeeId (required), department (required), hireDate (optional)</li>
 *   <li><b>PARENT:</b> phoneNumber (required), address (required), emergencyContact (required)</li>
 *   <li><b>STUDENT:</b> studentNumber (required), gradeLevel (required), dateOfBirth (required), guardianId (required)</li>
 * </ul>
 *
 * <p>Example usage for changing to Teacher role:
 * <pre>
 * {
 *   "newRole": "TEACHER",
 *   "employeeId": "EMP123",
 *   "department": "Mathematics",
 *   "hireDate": "2024-01-15"
 * }
 * </pre>
 */
@Schema(description = "Request payload for changing a user's role with new role-specific specialization data")
public record ChangeRoleRequest(
        @Schema(
                description = "The new role to assign to the user. Valid values: TEACHER, PARENT, STUDENT",
                example = "TEACHER",
                required = true)
        @NotBlank(message = "New role is required") String newRole,

        // Teacher data
        @Schema(
                description = "Unique employee identifier (required for TEACHER role)",
                example = "EMP2024-001",
                required = false)
        String employeeId,

        @Schema(
                description = "Department or faculty assignment (required for TEACHER role)",
                example = "Mathematics",
                required = false)
        String department,

        @Schema(
                description = "Hire date in ISO 8601 format (optional for TEACHER role)",
                example = "2024-01-15",
                required = false)
        String hireDate,

        // Parent data
        @Schema(
                description = "Contact phone number (required for PARENT role)",
                example = "+1-555-0123",
                required = false)
        String phoneNumber,

        @Schema(
                description = "Residential address (required for PARENT role)",
                example = "123 Main St, City, State 12345",
                required = false)
        String address,

        @Schema(
                description = "Emergency contact information (required for PARENT role)",
                example = "Jane Doe - +1-555-0987",
                required = false)
        String emergencyContact,

        // Student data
        @Schema(
                description = "Unique student identifier (required for STUDENT role)",
                example = "STU2024-001",
                required = false)
        String studentNumber,

        @Schema(
                description = "Grade level or year (required for STUDENT role)",
                example = "10th Grade",
                required = false)
        String gradeLevel,

        @Schema(
                description = "Date of birth in ISO 8601 format (required for STUDENT role)",
                example = "2010-05-15",
                required = false)
        String dateOfBirth,

        @Schema(
                description = "ID of the guardian parent (required for STUDENT role)",
                example = "123e4567-e89b-12d3-a456-426614174000",
                required = false)
        String guardianId) {}
