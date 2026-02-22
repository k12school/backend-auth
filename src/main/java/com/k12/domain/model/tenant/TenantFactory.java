package com.k12.domain.model.tenant;

import static com.k12.domain.model.tenant.error.TenantError.NameError.NAME_EMPTY;
import static com.k12.domain.model.tenant.error.TenantError.SubdomainError.SUBDOMAIN_INVALID_FORMAT;

import com.k12.domain.model.common.Result;
import com.k12.domain.model.common.TenantId;
import com.k12.domain.model.tenant.commands.TenantCommands.CreateTenant;
import com.k12.domain.model.tenant.error.TenantError;
import com.k12.domain.model.tenant.events.TenantEvents;
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
     * Creates a new tenant with validation.
     *
     * @param name Tenant's name string
     * @param subdomain Tenant's subdomain string
     * @return Result containing TenantCreated event on success, or TenantError on failure
     */
    public static Result<TenantEvents, TenantError> create(String name, String subdomain) {
        // Validate and create TenantName
        TenantName tenantName;
        try {
            tenantName = TenantName.of(name);
        } catch (IllegalArgumentException e) {
            return Result.failure(NAME_EMPTY);
        }

        // Validate and create Subdomain
        Subdomain subdomainObj;
        try {
            subdomainObj = Subdomain.of(subdomain);
        } catch (IllegalArgumentException e) {
            return Result.failure(SUBDOMAIN_INVALID_FORMAT);
        }

        return create(tenantName, subdomainObj);
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

        Tenant tenant = new Tenant(tenantId, name, subdomain, TenantStatus.ACTIVE);

        return Result.success(
                new TenantEvents.TenantCreated(tenantId, name, subdomain, TenantStatus.ACTIVE, now, version));
    }
}
