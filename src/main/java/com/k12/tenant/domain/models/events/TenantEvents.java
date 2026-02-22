package com.k12.tenant.domain.model.events;

import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.model.Subdomain;
import com.k12.tenant.domain.model.TenantName;
import com.k12.tenant.domain.model.TenantStatus;
import java.time.Instant;

/**
 * Domain events representing state changes in the Tenant aggregate.
 * Events contain ONLY the changed data (deltas), not full state.
 * Reconstruction applies events incrementally to build current state.
 */
public sealed interface TenantEvents
        permits TenantEvents.TenantCreated,
                TenantEvents.TenantSuspended,
                TenantEvents.TenantActivated,
                TenantEvents.TenantDeactivated,
                TenantEvents.TenantNameUpdated,
                TenantEvents.TenantSubdomainUpdated {

    /**
     * Tenant was created - contains initial state (all fields).
     * This is the only event that contains full tenant data.
     */
    record TenantCreated(
            TenantId tenantId,
            TenantName name,
            Subdomain subdomain,
            TenantStatus status,
            Instant createdAt,
            long version)
            implements TenantEvents {}

    /**
     * Tenant was suspended - only needs tenantId.
     */
    record TenantSuspended(TenantId tenantId, Instant suspendedAt, long version) implements TenantEvents {}

    /**
     * Tenant was activated - only needs tenantId.
     */
    record TenantActivated(TenantId tenantId, Instant activatedAt, long version) implements TenantEvents {}

    /**
     * Tenant was deactivated - only needs tenantId.
     */
    record TenantDeactivated(TenantId tenantId, Instant deactivatedAt, long version) implements TenantEvents {}

    /**
     * Tenant name was updated - contains both old and new name.
     */
    record TenantNameUpdated(
            TenantId tenantId, TenantName newName, String previousName, Instant updatedAt, long version)
            implements TenantEvents {}

    /**
     * Tenant subdomain was updated - contains both old and new subdomain.
     */
    record TenantSubdomainUpdated(
            TenantId tenantId, Subdomain newSubdomain, String previousSubdomain, Instant updatedAt, long version)
            implements TenantEvents {}
}
