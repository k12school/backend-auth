package com.k12.tenant.infrastructure.rest.dto;

import com.k12.common.domain.model.TenantId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Response payload containing created tenant administrator information")
public record TenantAdminResponse(
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String userId,

    @Schema(description = "User email", example = "admin@tenant.com")
    String email,

    @Schema(description = "User name", example = "Tenant Administrator")
    String name,

    @Schema(description = "User role (always ADMIN)", example = "ADMIN")
    String role,

    @Schema(description = "Associated tenant ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String tenantId,

    @Schema(description = "Admin ID (same as user ID)", example = "123e4567-e89b-12d3-a456-426614174000")
    String adminId,

    @Schema(description = "Admin permissions")
    Set<Permission> permissions,

    @Schema(description = "Creation timestamp (ISO 8601)", example = "2024-02-26T10:30:00Z")
    String createdAt
) {
    public static TenantAdminResponse from(User user, Admin admin, TenantId tenantId) {
        return new TenantAdminResponse(
            user.userId().value(),
            user.emailAddress().value(),
            user.name().value(),
            UserRole.ADMIN.name(),
            tenantId.value(),
            admin.adminId().userId().value(),
            admin.permissions(),
            admin.createdAt().toString()
        );
    }
}
