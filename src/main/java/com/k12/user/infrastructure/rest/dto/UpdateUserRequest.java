package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateUserRequest(
        @Email(message = "Email must be valid") String email,

        @Size(min = 2, message = "Name must be at least 2 characters") String name,

        // Teacher-specific fields
        String department,

        // Parent-specific fields
        String phoneNumber,
        String address,
        String emergencyContact,

        // Student-specific fields
        String gradeLevel,
        LocalDate dateOfBirth,
        String guardianId) {}
