package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Login request with email and password")
public record LoginRequestDTO(
        @Schema(description = "User email address", example = "admin@k12.com", required = true)
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

        @Schema(description = "User password", example = "admin123", required = true)
        @NotBlank(message = "Password is required") String password) {}
