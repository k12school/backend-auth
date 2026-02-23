package com.k12.tenant.domain.models;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.commands.TenantCommands.CreateTenant;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory for creating Tenant instances and handling CreateTenant commands.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TenantFactory {

    /**
     * Handles the CreateTenant command and returns a Result with the created tenant.
     *
     * @param command The CreateTenant command containing tenant data
     * @return Result containing TenantCreated event on success, or TenantError on failure
     */
    public static Result<TenantEvents, TenantError> handle(CreateTenant command) {
        return create(command.name(), command.subdomain());
    }

    /**
     * Creates a new tenant with validation using ROP pattern.
     *
     * @param name Tenant's name string
     * @param subdomain Tenant's subdomain string
     * @return Result containing TenantCreated event on success, or TenantError on failure
     */
    public static Result<TenantEvents, TenantError> create(String name, String subdomain) {
        // Validate and create TenantName
        var nameResult = TenantName.of(name);
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.getError());
        }

        // Validate and create Subdomain
        var subdomainResult = Subdomain.of(subdomain);
        if (subdomainResult.isFailure()) {
            return Result.failure(subdomainResult.getError());
        }

        return create(nameResult.get(), subdomainResult.get());
    }

    /**
     * Creates a new tenant from already validated value objects.
     * Use this when you already have validated inputs.
     *
     * @param name Validated tenant name
     * @param subdomain Validated subdomain
     * @return Result containing TenantCreated event on success, or TenantError on failure
     */
    public static Result<TenantEvents, TenantError> create(TenantName name, Subdomain subdomain) {
        TenantId tenantId = TenantId.generate();
        java.time.Instant now = java.time.Instant.now();
        long version = 1L;

        return Result.success(
                new TenantEvents.TenantCreated(tenantId, name, subdomain, TenantStatus.ACTIVE, now, version));
    }
}
