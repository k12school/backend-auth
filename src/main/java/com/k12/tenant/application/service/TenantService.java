package com.k12.tenant.application.service;

import static com.k12.tenant.domain.models.error.TenantError.ConflictError.NAME_ALREADY_EXISTS;
import static com.k12.tenant.domain.models.error.TenantError.ConflictError.SUBDOMAIN_ALREADY_EXISTS;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Tenant;
import com.k12.tenant.domain.models.TenantFactory;
import com.k12.tenant.domain.models.commands.TenantCommands;
import com.k12.tenant.domain.models.commands.TenantCommands.ActivateTenant;
import com.k12.tenant.domain.models.commands.TenantCommands.DeleteTenant;
import com.k12.tenant.domain.models.commands.TenantCommands.SuspendTenant;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import com.k12.tenant.domain.port.TenantRepository;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantRequest;
import io.micrometer.core.annotation.Timed;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Application service that orchestrates tenant operations.
 * Uses ROP pattern throughout with Result<T, E>.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Creates a new tenant.
     *
     * @param request The create tenant request
     * @return Result containing TenantCreated event or error
     */
    @Timed(
            value = "tenant.create",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to create a new tenant")
    public Result<TenantEvents, TenantError> createTenant(CreateTenantRequest request) {
        // Check for name conflicts
        var nameExistsResult = tenantRepository.nameExists(request.name());
        if (nameExistsResult.isFailure()) {
            return Result.failure(nameExistsResult.getError());
        }
        if (nameExistsResult.get()) {
            return Result.failure(NAME_ALREADY_EXISTS);
        }

        // Check for subdomain conflicts
        var subdomainExistsResult = tenantRepository.subdomainExists(request.subdomain());
        if (subdomainExistsResult.isFailure()) {
            return Result.failure(subdomainExistsResult.getError());
        }
        if (subdomainExistsResult.get()) {
            return Result.failure(SUBDOMAIN_ALREADY_EXISTS);
        }

        // Create tenant via factory
        var command = new TenantCommands.CreateTenant(request.name(), request.subdomain());
        var eventResult = TenantFactory.handle(command);
        if (eventResult.isFailure()) {
            return eventResult;
        }

        // Append event to event store (first event has version 1)
        var appendResult = tenantRepository.append(eventResult.get(), 1L);
        if (appendResult.isFailure()) {
            return Result.failure(appendResult.getError());
        }

        return eventResult;
    }

    /**
     * Loads a tenant by ID.
     *
     * @param tenantId The tenant ID
     * @return Result containing the tenant or error
     */
    @Timed(
            value = "tenant.get",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to load a tenant")
    public Result<Tenant, TenantError> getTenant(TenantId tenantId) {
        return tenantRepository.load(tenantId);
    }

    /**
     * Processes a command for a tenant.
     *
     * @param tenantId The tenant ID
     * @param command The command to process
     * @return Result containing the event or error
     */
    public Result<TenantEvents, TenantError> processCommand(TenantId tenantId, TenantCommands command) {
        // Load tenant (which includes version)
        var tenantResult = tenantRepository.load(tenantId);
        if (tenantResult.isFailure()) {
            return Result.failure(tenantResult.getError());
        }

        Tenant tenant = tenantResult.get();

        // Process command (event will have tenant.version + 1)
        var eventResult = tenant.process(command);
        if (eventResult.isFailure()) {
            return eventResult;
        }

        // Append event with version from the event itself
        long eventVersion = extractVersion(eventResult.get());
        var appendResult = tenantRepository.append(eventResult.get(), eventVersion);
        if (appendResult.isFailure()) {
            return Result.failure(appendResult.getError());
        }

        return eventResult;
    }

    /**
     * Extracts version from an event.
     */
    private long extractVersion(TenantEvents event) {
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

    /**
     * Activates a tenant.
     */
    @Timed(
            value = "tenant.activate",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to activate a tenant")
    public Result<TenantEvents, TenantError> activateTenant(TenantId tenantId) {
        return processCommand(tenantId, new ActivateTenant(tenantId));
    }

    /**
     * Suspends a tenant.
     */
    @Timed(
            value = "tenant.suspend",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to suspend a tenant")
    public Result<TenantEvents, TenantError> suspendTenant(TenantId tenantId) {
        return processCommand(tenantId, new SuspendTenant(tenantId));
    }

    /**
     * Deletes a tenant.
     */
    public Result<TenantEvents, TenantError> deleteTenant(TenantId tenantId) {
        return processCommand(tenantId, new DeleteTenant(tenantId));
    }
}
