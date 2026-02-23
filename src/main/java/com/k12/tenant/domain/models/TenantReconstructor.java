package com.k12.tenant.domain.models;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Reconstructs Tenant state from a sequence of delta events.
 * Events contain only changes, not full state.
 * Reconstruction applies events incrementally.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TenantReconstructor {

    /**
     * Reconstructs a Tenant from a list of delta events.
     * Events must be in chronological order and must start with TenantCreated.
     *
     * @param events List of events in chronological order
     * @return Result containing the reconstructed Tenant on success, or TenantError on failure
     */
    public static Result<Tenant, TenantError> reconstruct(List<TenantEvents> events) {
        if (events == null || events.isEmpty()) {
            return Result.failure(TenantError.ValidationError.INVALID_VALUE);
        }

        Result<Tenant, TenantError> currentResult = null;
        TenantId tenantId = null;

        for (TenantEvents event : events) {
            // Validate event belongs to same aggregate
            if (tenantId != null) {
                TenantId eventTenantId = extractTenantId(event);
                if (!eventTenantId.equals(tenantId)) {
                    return Result.failure(TenantError.ValidationError.INVALID_VALUE);
                }
            }

            var applyResult =
                    applyEvent(currentResult != null && currentResult.isSuccess() ? currentResult.get() : null, event);

            if (applyResult.isFailure()) {
                return applyResult;
            }

            currentResult = applyResult;

            // Track tenantId after first event
            if (tenantId == null && currentResult.isSuccess()) {
                tenantId = currentResult.get().id();
            }
        }

        if (currentResult == null || currentResult.isFailure()) {
            return Result.failure(TenantError.ValidationError.INVALID_VALUE);
        }

        return currentResult;
    }

    /**
     * Reconstructs a Tenant from events with full validation.
     * Validates:
     * - Events are in chronological order
     * - Versions are sequential
     * - First event is TenantCreated
     * - All events for same aggregate
     *
     * @param events List of events in chronological order
     * @return Result containing the reconstructed Tenant on success, or TenantError on failure
     */
    public static Result<Tenant, TenantError> reconstructWithValidation(List<TenantEvents> events) {
        if (events == null || events.isEmpty()) {
            return Result.failure(TenantError.ValidationError.INVALID_VALUE);
        }

        // First event must be TenantCreated
        if (!(events.getFirst() instanceof TenantEvents.TenantCreated)) {
            return Result.failure(TenantError.ValidationError.VALUE_REQUIRED);
        }

        Result<Tenant, TenantError> currentResult = null;
        TenantId tenantId = null;
        Long expectedVersion = 1L;

        for (TenantEvents event : events) {
            // Validate version sequence
            Long eventVersion = extractVersion(event);
            if (eventVersion != null && !eventVersion.equals(expectedVersion)) {
                return Result.failure(TenantError.ValidationError.INVALID_VALUE);
            }

            // Validate same aggregate
            if (tenantId != null) {
                TenantId eventTenantId = extractTenantId(event);
                if (!eventTenantId.equals(tenantId)) {
                    return Result.failure(TenantError.ValidationError.INVALID_VALUE);
                }
            }

            var applyResult =
                    applyEvent(currentResult != null && currentResult.isSuccess() ? currentResult.get() : null, event);

            if (applyResult.isFailure()) {
                return applyResult;
            }

            currentResult = applyResult;

            if (tenantId == null && currentResult.isSuccess()) {
                tenantId = currentResult.get().id();
            }

            expectedVersion++;
        }

        if (currentResult == null || currentResult.isFailure()) {
            return Result.failure(TenantError.ValidationError.INVALID_VALUE);
        }

        return currentResult;
    }

    /**
     * Applies a single delta event to a Tenant, returning the updated Tenant.
     * Handles incremental state changes using ROP pattern.
     *
     * @param tenant Current tenant state (null for TenantCreated)
     * @param event Event to apply
     * @return Result containing updated tenant on success, or TenantError on failure
     */
    public static Result<Tenant, TenantError> applyEvent(Tenant tenant, TenantEvents event) {
        return switch (event) {
            // TenantCreated: Creates new tenant from initial state
            case TenantEvents.TenantCreated(
                    TenantId tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    long version) -> Result.success(new Tenant(tenantId, name, subdomain, status, version));

            // TenantSuspended: Changes status to SUSPENDED and updates version
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, long version)
            when tenant != null ->
                Result.success(
                        new Tenant(tenant.id(), tenant.name(), tenant.subdomain(), TenantStatus.SUSPENDED, version));

            // TenantActivated: Changes status to ACTIVE and updates version
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, long version)
            when tenant != null ->
                Result.success(
                        new Tenant(tenant.id(), tenant.name(), tenant.subdomain(), TenantStatus.ACTIVE, version));

            // TenantDeactivated: Changes status to INACTIVE and updates version
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, long version)
            when tenant != null ->
                Result.success(
                        new Tenant(tenant.id(), tenant.name(), tenant.subdomain(), TenantStatus.INACTIVE, version));

            // TenantDeleted: Marks tenant as deleted (status remains unchanged), updates version
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, long version)
            when tenant != null ->
                Result.success(new Tenant(tenant.id(), tenant.name(), tenant.subdomain(), tenant.status(), version));

            // TenantNameUpdated: Changes name and updates version
            case TenantEvents.TenantNameUpdated(
                    var tenantId,
                    var newName,
                    var previousName,
                    var updatedAt,
                    long version)
            when tenant != null ->
                Result.success(new Tenant(tenant.id(), newName, tenant.subdomain(), tenant.status(), version));

            // TenantSubdomainUpdated: Changes subdomain and updates version
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    long version)
            when tenant != null ->
                Result.success(new Tenant(tenant.id(), tenant.name(), newSubdomain, tenant.status(), version));

            // Handle null tenant case (shouldn't happen with proper event stream)
            default -> Result.failure(TenantError.ValidationError.INVALID_VALUE);
        };
    }

    /**
     * Extracts the TenantId from an event.
     */
    private static TenantId extractTenantId(TenantEvents event) {
        return switch (event) {
            case TenantEvents.TenantCreated(
                    var tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    var version) -> tenantId;
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, var version) -> tenantId;
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, var version) -> tenantId;
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, var version) -> tenantId;
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, var version) -> tenantId;
            case TenantEvents.TenantNameUpdated(
                    var tenantId,
                    var newName,
                    var previousName,
                    var updatedAt,
                    var version) -> tenantId;
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    var version) -> tenantId;
        };
    }

    /**
     * Extracts the version from an event (if present).
     */
    private static Long extractVersion(TenantEvents event) {
        return switch (event) {
            case TenantEvents.TenantCreated(
                    var tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    long version) -> version;
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, long version) -> version;
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, long version) -> version;
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, long version) -> version;
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, long version) -> version;
            case TenantEvents.TenantNameUpdated(
                    var tenantId,
                    var newName,
                    var previousName,
                    var updatedAt,
                    long version) -> version;
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    long version) -> version;
        };
    }
}
