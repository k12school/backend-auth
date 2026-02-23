package com.k12.tenant.domain.port;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Tenant;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for Tenant persistence operations.
 * All methods return Result<T, E> following ROP pattern.
 */
public interface TenantRepository {

    /**
     * Appends a new event to the event store with optimistic locking.
     *
     * @param event The event to append
     * @param expectedVersion The expected version (for optimistic locking)
     * @return Result containing success or ConcurrencyError on version mismatch
     */
    Result<Void, TenantError> append(TenantEvents event, long expectedVersion);

    /**
     * Loads all events for a tenant, ordered by version.
     *
     * @param tenantId The tenant ID
     * @return Result containing list of events or PersistenceError on failure
     */
    Result<List<TenantEvents>, TenantError> loadEvents(TenantId tenantId);

    /**
     * Loads the current tenant state by reconstructing from events.
     *
     * @param tenantId The tenant ID
     * @return Result containing the tenant or appropriate error
     */
    Result<Tenant, TenantError> load(TenantId tenantId);

    /**
     * Gets the current version of a tenant aggregate.
     *
     * @param tenantId The tenant ID
     * @return Result containing the current version or error
     */
    Result<Long, TenantError> getCurrentVersion(TenantId tenantId);

    /**
     * Checks if a tenant name already exists.
     *
     * @param name The tenant name
     * @return Result containing true if name exists, false otherwise
     */
    Result<Boolean, TenantError> nameExists(String name);

    /**
     * Checks if a subdomain already exists.
     *
     * @param subdomain The subdomain
     * @return Result containing true if subdomain exists, false otherwise
     */
    Result<Boolean, TenantError> subdomainExists(String subdomain);

    /**
     * Finds a tenant ID by name.
     *
     * @param name The tenant name
     * @return Optional containing the tenant ID if found
     */
    Optional<TenantId> findByName(String name);

    /**
     * Finds a tenant ID by subdomain.
     *
     * @param subdomain The subdomain
     * @return Optional containing the tenant ID if found
     */
    Optional<TenantId> findBySubdomain(String subdomain);
}
