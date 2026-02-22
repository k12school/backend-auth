package com.k12.tenant.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new tenant.
 */
@Schema(description = "Request payload for creating a new tenant")
public record CreateTenantRequest(
        @NotBlank(message = "Name is required")
        @Schema(
                description = "The name of the tenant",
                example = "Acme Corporation",
                requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @NotBlank(message = "Subdomain is required")
        @Schema(
                description = "The unique subdomain for the tenant",
                example = "acme",
                requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$",
                minLength = 1,
                maxLength = 63)
                String subdomain) {}
