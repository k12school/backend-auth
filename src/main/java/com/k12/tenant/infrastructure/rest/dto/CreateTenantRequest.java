package com.k12.tenant.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request DTO for creating a new tenant.
 */
@Schema(description = "Request payload for creating a new tenant")
public record CreateTenantRequest(
        @NotBlank(message = "Name is required") @Schema(description = "The name of the tenant", examples = "Acme Corporation", required = true)
        String name,

        @NotBlank(message = "Subdomain is required") @Schema(
                description = "The unique subdomain for the tenant",
                examples = "acme",
                required = true,
                pattern = "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
        String subdomain) {}
