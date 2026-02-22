package com.k12.tenant.domain.models;

import static com.k12.tenant.domain.models.error.TenantError.NameError.NAME_SAME_AS_CURRENT;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_SAME_AS_CURRENT;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.CANNOT_ACTIVATE_INACTIVE;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.TENANT_ALREADY_ACTIVE;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.TENANT_ALREADY_INACTIVE;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.TENANT_ALREADY_SUSPENDED;
import static java.time.Instant.now;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.commands.TenantCommands;
import com.k12.tenant.domain.models.commands.TenantCommands.ActivateTenant;
import com.k12.tenant.domain.models.commands.TenantCommands.DeactivateTenant;
import com.k12.tenant.domain.models.commands.TenantCommands.SuspendTenant;
import com.k12.tenant.domain.models.commands.TenantCommands.UpdateName;
import com.k12.tenant.domain.models.commands.TenantCommands.UpdateSubdomain;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import com.k12.tenant.domain.models.events.TenantEvents.TenantActivated;
import com.k12.tenant.domain.models.events.TenantEvents.TenantNameUpdated;
import com.k12.tenant.domain.models.events.TenantEvents.TenantSubdomainUpdated;
import com.k12.tenant.domain.models.events.TenantEvents.TenantSuspended;
import lombok.With;

public record Tenant(
        TenantId id,
        @With TenantName name,
        @With Subdomain subdomain,
        @With TenantStatus status) {

    public Result<TenantEvents, TenantError> process(TenantCommands command) {
        return switch (command) {
            case ActivateTenant activateTenant -> process(activateTenant);
            case SuspendTenant suspendTenant -> process(suspendTenant);
            case DeactivateTenant deactivateTenant -> process(deactivateTenant);
            case UpdateName updateName -> process(updateName);
            case UpdateSubdomain updateSubdomain -> process(updateSubdomain);
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    /**
     * Suspends the tenant if not already suspended.
     * @return Result containing TenantSuspended event on success, or TenantStatusError on failure
     */
    private Result<TenantEvents, TenantError> process(SuspendTenant command) {
        if (this.status == TenantStatus.SUSPENDED) {
            return Result.failure(TENANT_ALREADY_SUSPENDED);
        }
        return Result.success(new TenantSuspended(
                this.id(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Activates the tenant if not already active.
     * @return Result containing TenantActivated event on success, or TenantStatusError on failure
     */
    private Result<TenantEvents, TenantError> process(ActivateTenant command) {
        if (this.status == TenantStatus.ACTIVE) {
            return Result.failure(TENANT_ALREADY_ACTIVE);
        }
        if (this.status == TenantStatus.INACTIVE) {
            return Result.failure(CANNOT_ACTIVATE_INACTIVE);
        }
        return Result.success(new TenantActivated(
                this.id(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Deactivates the tenant (soft delete) if not already inactive.
     * @return Result containing TenantDeactivated event on success, or TenantStatusError on failure
     */
    private Result<TenantEvents, TenantError> process(DeactivateTenant command) {
        if (this.status == TenantStatus.INACTIVE) {
            return Result.failure(TENANT_ALREADY_INACTIVE);
        }
        return Result.success(new TenantEvents.TenantDeactivated(
                this.id(), now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Updates the tenant's name with validation.
     * @return Result containing TenantNameUpdated event on success, or NameError on failure
     */
    private Result<TenantEvents, TenantError> process(UpdateName command) {
        if (command.newName().value().equals(this.name.value())) {
            return Result.failure(NAME_SAME_AS_CURRENT);
        }

        String previousName = this.name.value();
        return Result.success(new TenantNameUpdated(
                this.id(), command.newName(), previousName, now(), System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Updates the tenant's subdomain with validation.
     * @return Result containing TenantSubdomainUpdated event on success, or SubdomainError on failure
     */
    private Result<TenantEvents, TenantError> process(UpdateSubdomain command) {
        if (command.newSubdomain().value().equals(this.subdomain.value())) {
            return Result.failure(SUBDOMAIN_SAME_AS_CURRENT);
        }

        String previousSubdomain = this.subdomain.value();
        return Result.success(new TenantSubdomainUpdated(
                this.id(),
                command.newSubdomain(),
                previousSubdomain,
                now(),
                System.currentTimeMillis() // version placeholder
                ));
    }

    /**
     * Applies an event to this tenant, returning the updated tenant.
     * This is useful for event sourcing and replaying events.
     *
     * @param event The event to apply
     * @return The updated tenant with the event applied
     */
    public Tenant apply(TenantEvents event) {
        return TenantReconstructor.applyEvent(this, event);
    }
}
