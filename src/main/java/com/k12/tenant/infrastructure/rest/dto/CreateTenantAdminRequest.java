package com.k12.tenant.infrastructure.rest.dto;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request payload for creating a tenant administrator")
public record CreateTenantAdminRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Schema(description = "Admin email address", example = "admin@tenant.com", required = true)
        String email,

        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") @Schema(description = "Admin password (will be hashed)", example = "SecurePass123", required = true)
        String password,

        @NotBlank(message = "Name is required") @Schema(description = "Admin full name", example = "Tenant Administrator", required = true)
        String name,

        @NotEmpty(message = "At least one permission is required") @Schema(description = "Set of permissions for this admin", required = true)
        Set<Permission> permissions) {}
