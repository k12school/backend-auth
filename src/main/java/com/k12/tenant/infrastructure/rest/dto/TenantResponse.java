package com.k12.tenant.infrastructure.rest.dto;

import com.k12.tenant.domain.models.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for tenant data.
 */
@Schema(description = "Response payload containing tenant information")
public record TenantResponse(
        @Schema(description = "Unique identifier of the tenant", example = "123e4567-e89b-12d3-a456-426614174000") String id,
        @Schema(description = "Name of the tenant", example = "Acme Corporation") String name,
        @Schema(description = "Subdomain of the tenant", example = "acme") String subdomain,
        @Schema(description = "Current status of the tenant", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED", "INACTIVE", "DELETED"}) String status,
        @Schema(description = "Timestamp when the tenant was created (ISO 8601 format)", example = "2024-01-15T10:30:00Z") String createdAt) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.id().value(),
                tenant.name().value(),
                tenant.subdomain().value(),
                tenant.status().name(),
                null); // createdAt will be set from projection
    }
}
