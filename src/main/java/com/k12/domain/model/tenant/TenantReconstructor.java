package com.k12.domain.model.tenant;

import com.k12.domain.model.common.Result;
import com.k12.domain.model.common.TenantId;
import com.k12.domain.model.tenant.error.TenantError;
import com.k12.domain.model.tenant.events.TenantEvents;
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

        Tenant currentTenant = null;
        TenantId tenantId = null;

        for (TenantEvents event : events) {
            // Validate event belongs to same aggregate
            if (tenantId != null) {
                TenantId eventTenantId = extractTenantId(event);
                if (!eventTenantId.equals(tenantId)) {
                    return Result.failure(TenantError.ValidationError.INVALID_VALUE);
                }
            }

            currentTenant = applyEvent(currentTenant, event);

            // Track tenantId after first event
            if (tenantId == null && currentTenant != null) {
                tenantId = currentTenant.id();
            }
        }

        if (currentTenant == null) {
            return Result.failure(TenantError.ValidationError.INVALID_VALUE);
        }

        return Result.success(currentTenant);
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

        Tenant currentTenant = null;
        TenantId tenantId = null;
        long expectedVersion = 1;

        for (TenantEvents event : events) {
            // Validate version sequence
            Long eventVersion = extractVersion(event);
            if (eventVersion != null && eventVersion != expectedVersion) {
                return Result.failure(TenantError.ValidationError.INVALID_VALUE);
            }

            // Validate same aggregate
            if (tenantId != null) {
                TenantId eventTenantId = extractTenantId(event);
                if (!eventTenantId.equals(tenantId)) {
                    return Result.failure(TenantError.ValidationError.INVALID_VALUE);
                }
            }

            currentTenant = applyEvent(currentTenant, event);

            if (tenantId == null && currentTenant != null) {
                tenantId = currentTenant.id();
            }

            expectedVersion++;
        }

        if (currentTenant == null) {
            return Result.failure(TenantError.ValidationError.INVALID_VALUE);
        }

        return Result.success(currentTenant);
    }

    /**
     * Applies a single delta event to a Tenant, returning the updated Tenant.
     * Handles incremental state changes.
     *
     * @param tenant Current tenant state (null for TenantCreated)
     * @param event Event to apply
     * @return Updated tenant with the event applied
     */
    public static Tenant applyEvent(Tenant tenant, TenantEvents event) {
        return switch (event) {
            // TenantCreated: Creates new tenant from initial state
            case TenantEvents.TenantCreated(
                    TenantId tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    var version) -> new Tenant(tenantId, name, subdomain, status);

            // TenantSuspended: Changes status to SUSPENDED
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, var version)
            when tenant != null -> tenant.withStatus(TenantStatus.SUSPENDED);

            // TenantActivated: Changes status to ACTIVE
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, var version)
            when tenant != null -> tenant.withStatus(TenantStatus.ACTIVE);

            // TenantDeactivated: Changes status to INACTIVE
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, var version)
            when tenant != null -> tenant.withStatus(TenantStatus.INACTIVE);

            // TenantNameUpdated: Changes name
            case TenantEvents.TenantNameUpdated(var tenantId, var newName, var previousName, var updatedAt, var version)
            when tenant != null -> tenant.withName(newName);

            // TenantSubdomainUpdated: Changes subdomain
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    var version)
            when tenant != null -> tenant.withSubdomain(newSubdomain);

            // Handle null tenant case (shouldn't happen with proper event stream)
            default -> throw new IllegalStateException("Cannot apply event without existing tenant: " + event);
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
