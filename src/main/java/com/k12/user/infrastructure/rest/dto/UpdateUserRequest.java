package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request DTO for updating user fields within the current role.
 *
 * <p>This record is used for PATCH /api/users/{id} endpoint to support partial field updates.
 * All fields are optional - only non-null fields will be updated. This DTO does NOT support
 * role changes; use {@link ChangeRoleRequest} for that purpose.
 *
 * <p>Validation behavior:
 * <ul>
 *   <li>Email and name have validation constraints that apply only when the field is provided</li>
 *   <li>Role-specific fields are only applicable to users with that role</li>
 *   <li>Omitting a field means it will not be updated</li>
 * </ul>
 *
 * <p>Role-specific field applicability:
 * <ul>
 *   <li><b>Teacher:</b> department</li>
 *   <li><b>Parent:</b> phoneNumber, address, emergencyContact</li>
 *   <li><b>Student:</b> gradeLevel, dateOfBirth, guardianId</li>
 * </ul>
 */
@Schema(description = "Request payload for partial updates to user fields within current role")
public record UpdateUserRequest(
        @Schema(description = "Email address for the user", example = "john.doe@example.com", required = false)
        @Email(message = "Email must be valid") String email,

        @Schema(description = "Full name of the user", example = "John Doe", required = false)
        @Size(min = 2, message = "Name must be at least 2 characters") String name,

        @Schema(
                description = "Department or faculty (Teacher-specific field)",
                example = "Mathematics",
                required = false)
        String department,

        @Schema(description = "Contact phone number (Parent-specific field)", example = "+1-555-0123", required = false)
        String phoneNumber,

        @Schema(
                description = "Residential address (Parent-specific field)",
                example = "123 Main St, City, State 12345",
                required = false)
        String address,

        @Schema(
                description = "Emergency contact information (Parent-specific field)",
                example = "Jane Doe - +1-555-0987",
                required = false)
        String emergencyContact,

        @Schema(description = "Grade level or year (Student-specific field)", example = "10th Grade", required = false)
        String gradeLevel,

        @Schema(
                description = "Date of birth in ISO 8601 format (Student-specific field)",
                example = "2010-05-15",
                required = false)
        LocalDate dateOfBirth,

        @Schema(
                description = "ID of the guardian parent (Student-specific field)",
                example = "123e4567-e89b-12d3-a456-426614174000",
                required = false)
        String guardianId) {}
