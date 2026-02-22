package com.k12.tenant.domain.models.commands;

import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Subdomain;
import com.k12.tenant.domain.models.TenantName;

/**
 * Sealed interface representing all possible tenant commands.
 * Each command is a record containing the data needed to execute it.
 */
public sealed interface TenantCommands
        permits TenantCommands.CreateTenant,
                TenantCommands.SuspendTenant,
                TenantCommands.ActivateTenant,
                TenantCommands.UpdateName,
                TenantCommands.UpdateSubdomain,
                TenantCommands.DeactivateTenant {

    /**
     * Command to create a new tenant.
     */
    record CreateTenant(String name, String subdomain) implements TenantCommands {}

    /**
     * Command to suspend a tenant.
     */
    record SuspendTenant(TenantId tenantId) implements TenantCommands {}

    /**
     * Command to activate a tenant.
     */
    record ActivateTenant(TenantId tenantId) implements TenantCommands {}

    /**
     * Command to deactivate a tenant (soft delete).
     */
    record DeactivateTenant(TenantId tenantId) implements TenantCommands {}

    /**
     * Command to update a tenant's name.
     */
    record UpdateName(TenantId tenantId, TenantName newName) implements TenantCommands {}

    /**
     * Command to update a tenant's subdomain.
     */
    record UpdateSubdomain(TenantId tenantId, Subdomain newSubdomain) implements TenantCommands {}
}
