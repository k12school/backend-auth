package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
        @NotBlank(message = "New role is required") String newRole,

        // Teacher data
        String employeeId,
        String department,
        String hireDate,

        // Parent data
        String phoneNumber,
        String address,
        String emergencyContact,

        // Student data
        String studentNumber,
        String gradeLevel,
        String dateOfBirth,
        String guardianId) {}
