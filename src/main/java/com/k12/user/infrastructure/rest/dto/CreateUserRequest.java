package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for creating a user with role and specialization.
 */
public record CreateUserRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

        @NotBlank(message = "Name is required") String name,

        @NotNull(message = "Role is required") UserRole role,

        TeacherData teacherData,

        ParentData parentData,

        StudentData studentData) {

    public record UserRole(String value) {
        public UserRole {
            if (!List.of("TEACHER", "PARENT", "STUDENT", "ADMIN").contains(value)) {
                throw new IllegalArgumentException("Invalid role: " + value);
            }
        }
    }

    public record TeacherData(
            @NotBlank(message = "Employee ID is required") String employeeId, String department, String hireDate) {}

    public record ParentData(String phoneNumber, String address, String emergencyContact) {}

    public record StudentData(
            @NotBlank(message = "Student number is required") String studentNumber,

            @NotBlank(message = "Grade level is required") String gradeLevel,

            @NotNull(message = "Date of birth is required") String dateOfBirth,

            String guardianId) {}
}
