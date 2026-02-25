package com.k12.tenant.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.TenantEvents.TENANT_EVENTS;
import static com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS;
import static com.k12.tenant.domain.models.error.TenantError.ConcurrencyError.VERSION_CONFLICT;
import static com.k12.tenant.domain.models.error.TenantError.PersistenceError.STORAGE_ERROR;

import com.k12.backend.infrastructure.jooq.public_.tables.records.TenantEventsRecord;
import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Tenant;
import com.k12.tenant.domain.models.TenantReconstructor;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import com.k12.tenant.domain.port.TenantRepository;
import io.agroal.api.AgroalDataSource;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.impl.DSL;

/**
 * Implementation of TenantRepository using jOOQ with transactional correctness.
 * All methods return Result<T, E> following ROP pattern.
 *
 * <p>Version semantics: The caller provides the expected NEW version number.
 * The database enforces uniqueness via the (tenant_id, version) constraint.
 * If an event with the same version already exists, the insert fails with VERSION_CONFLICT.
 *
 * <p>Transactions: Uses Quarkus @Transactional to ensure atomicity.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final AgroalDataSource dataSource;
    private final MeterRegistry meterRegistry;

    @Override
    @Timed(
            value = "db.tenant.append",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to append event to database")
    @Transactional
    public Result<Void, TenantError> append(TenantEvents event, long expectedVersion) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            // Insert event with conflict detection
            // The unique constraint (tenant_id, version) ensures optimistic locking
            OffsetDateTime occurredAt = OffsetDateTime.ofInstant(extractOccurredAt(event), ZoneOffset.UTC);
            int inserted = ctx.insertInto(
                            TENANT_EVENTS,
                            TENANT_EVENTS.TENANT_ID,
                            TENANT_EVENTS.EVENT_TYPE,
                            TENANT_EVENTS.EVENT_DATA,
                            TENANT_EVENTS.VERSION,
                            TENANT_EVENTS.OCCURRED_AT)
                    .values(
                            extractTenantIdUUID(event),
                            event.getClass().getSimpleName(),
                            KryoEventSerializer.serialize(event),
                            expectedVersion,
                            occurredAt)
                    .onConflict(TENANT_EVENTS.TENANT_ID, TENANT_EVENTS.VERSION)
                    .doNothing()
                    .execute();

            // If 0 rows inserted, version already exists => conflict
            if (inserted == 0) {
                recordError("VERSION_CONFLICT");
                return Result.failure(VERSION_CONFLICT);
            }

            // Update projection in the same transaction
            updateProjection(ctx, event);

            return Result.success(null);
        } catch (IntegrityConstraintViolationException e) {
            // Catch any other constraint violations
            recordError("STORAGE_ERROR_CONSTRAINT");
            return Result.failure(STORAGE_ERROR);
        } catch (Exception e) {
            recordError("STORAGE_ERROR_EXCEPTION");
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    @Timed(
            value = "db.tenant.loadEvents",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to load events from database")
    public Result<List<TenantEvents>, TenantError> loadEvents(TenantId tenantId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            List<TenantEventsRecord> records = ctx.selectFrom(TENANT_EVENTS)
                    .where(TENANT_EVENTS.TENANT_ID.eq(UUID.fromString(tenantId.value())))
                    .orderBy(TENANT_EVENTS.VERSION.asc())
                    .fetch();

            List<TenantEvents> events = new ArrayList<>();
            for (TenantEventsRecord record : records) {
                byte[] eventData = record.getEventData();
                events.add(KryoEventSerializer.deserialize(eventData));
            }

            // Record event count for this tenant
            int eventCount = events.size();
            recordEventCount(tenantId.value(), eventCount);

            return Result.success(events);
        } catch (Exception e) {
            recordError("LOAD_EVENTS_ERROR");
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    @Timed(
            value = "db.tenant.load",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to load and reconstruct tenant")
    public Result<Tenant, TenantError> load(TenantId tenantId) {
        var eventsResult = loadEvents(tenantId);
        if (eventsResult.isFailure()) {
            return Result.failure(eventsResult.getError());
        }

        return TenantReconstructor.reconstructWithValidation(eventsResult.get());
    }

    @Override
    public Result<Long, TenantError> getCurrentVersion(TenantId tenantId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            Long version = ctx.select(DSL.max(TENANT_EVENTS.VERSION))
                    .from(TENANT_EVENTS)
                    .where(TENANT_EVENTS.TENANT_ID.eq(UUID.fromString(tenantId.value())))
                    .fetchOneInto(Long.class);

            if (version == null) {
                return Result.failure(TenantError.ValidationError.VALUE_REQUIRED);
            }

            return Result.success(version);
        } catch (Exception e) {
            recordError("GET_VERSION_ERROR");
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    @Timed(
            value = "db.tenant.nameExists",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to check if tenant name exists")
    public Result<Boolean, TenantError> nameExists(String name) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            boolean exists = ctx.fetchExists(ctx.selectOne().from(TENANTS).where(TENANTS.NAME.eq(name)));

            return Result.success(exists);
        } catch (Exception e) {
            recordError("NAME_EXISTS_ERROR");
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    @Timed(
            value = "db.tenant.subdomainExists",
            percentiles = {0.5, 0.95, 0.99},
            description = "Time to check if subdomain exists")
    public Result<Boolean, TenantError> subdomainExists(String subdomain) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            boolean exists = ctx.fetchExists(ctx.selectOne().from(TENANTS).where(TENANTS.SUBDOMAIN.eq(subdomain)));

            return Result.success(exists);
        } catch (Exception e) {
            recordError("SUBDOMAIN_EXISTS_ERROR");
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    public Optional<TenantId> findByName(String name) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            UUID id = ctx.select(TENANTS.ID)
                    .from(TENANTS)
                    .where(TENANTS.NAME.eq(name))
                    .fetchOne(TENANTS.ID);

            return id != null ? Optional.of(new TenantId(id.toString())) : Optional.empty();
        } catch (Exception e) {
            recordError("FIND_BY_NAME_ERROR");
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantId> findBySubdomain(String subdomain) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            UUID id = ctx.select(TENANTS.ID)
                    .from(TENANTS)
                    .where(TENANTS.SUBDOMAIN.eq(subdomain))
                    .fetchOne(TENANTS.ID);

            return id != null ? Optional.of(new TenantId(id.toString())) : Optional.empty();
        } catch (Exception e) {
            recordError("FIND_BY_SUBDOMAIN_ERROR");
            return Optional.empty();
        }
    }

    /**
     * Record error for metrics tracking.
     */
    private void recordError(String errorType) {
        Counter.builder("tenant.repository.errors")
                .description("Repository errors by type")
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record event count for a tenant.
     */
    private void recordEventCount(String tenantId, int count) {
        // Use AtomicInteger to allow Gauge to track changes
        Gauge.builder("tenant.events.count", () -> count)
                .description("Number of events for tenant")
                .tags("tenant_id", tenantId)
                .strongReference(true)
                .register(meterRegistry);
    }

    /**
     * Updates the read projection after appending an event.
     *
     * @param ctx The DSLContext
     * @param event The event to project
     */
    private void updateProjection(DSLContext ctx, TenantEvents event) {
        switch (event) {
            case TenantEvents.TenantCreated(
                    var tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    var version) -> {
                OffsetDateTime timestamp = OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC);
                ctx.insertInto(
                                TENANTS,
                                TENANTS.ID,
                                TENANTS.NAME,
                                TENANTS.SUBDOMAIN,
                                TENANTS.STATUS,
                                TENANTS.VERSION,
                                TENANTS.CREATED_AT,
                                TENANTS.UPDATED_AT)
                        .values(
                                UUID.fromString(tenantId.value()),
                                name.value(),
                                subdomain.value(),
                                status.name(),
                                version,
                                timestamp,
                                timestamp)
                        .onConflict(TENANTS.ID)
                        .doNothing()
                        .execute();
            }
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, var version) ->
                updateTenantWithVersionCheck(ctx, UUID.fromString(tenantId.value()), version, suspendedAt, "SUSPENDED");
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, var version) ->
                updateTenantWithVersionCheck(ctx, UUID.fromString(tenantId.value()), version, activatedAt, "ACTIVE");
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, var version) ->
                updateTenantWithVersionCheck(
                        ctx, UUID.fromString(tenantId.value()), version, deactivatedAt, "INACTIVE");
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, var version) -> {
                ctx.deleteFrom(TENANTS)
                        .where(TENANTS.ID.eq(UUID.fromString(tenantId.value())))
                        .execute();
            }
            case TenantEvents.TenantNameUpdated(var tenantId, var newName, var _, var updatedAt, var version) -> {
                OffsetDateTime timestamp = OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC);
                ctx.update(TENANTS)
                        .set(TENANTS.NAME, newName.value())
                        .set(TENANTS.VERSION, version)
                        .set(TENANTS.UPDATED_AT, timestamp)
                        .where(TENANTS.ID.eq(UUID.fromString(tenantId.value())))
                        .execute();
            }
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var _,
                    var updatedAt,
                    var version) -> {
                OffsetDateTime timestamp = OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC);
                ctx.update(TENANTS)
                        .set(TENANTS.SUBDOMAIN, newSubdomain.value())
                        .set(TENANTS.VERSION, version)
                        .set(TENANTS.UPDATED_AT, timestamp)
                        .where(TENANTS.ID.eq(UUID.fromString(tenantId.value())))
                        .execute();
            }
        }
    }

    /**
     * Helper to update tenant status.
     *
     * @param ctx The DSLContext
     * @param tenantId The tenant ID
     * @param newVersion The new version
     * @param updatedAt The update timestamp
     * @param newStatus The new status
     */
    private void updateTenantWithVersionCheck(
            DSLContext ctx, UUID tenantId, long newVersion, Instant updatedAt, String newStatus) {

        OffsetDateTime timestamp = OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC);
        ctx.update(TENANTS)
                .set(TENANTS.STATUS, newStatus)
                .set(TENANTS.VERSION, newVersion)
                .set(TENANTS.UPDATED_AT, timestamp)
                .where(TENANTS.ID.eq(tenantId))
                .execute();
    }

    private UUID extractTenantIdUUID(TenantEvents event) {
        return switch (event) {
            case TenantEvents.TenantCreated(var tenantId, var _, var _, var _, var _, var _) ->
                UUID.fromString(tenantId.value());
            case TenantEvents.TenantSuspended(var tenantId, var _, var _) -> UUID.fromString(tenantId.value());
            case TenantEvents.TenantActivated(var tenantId, var _, var _) -> UUID.fromString(tenantId.value());
            case TenantEvents.TenantDeactivated(var tenantId, var _, var _) -> UUID.fromString(tenantId.value());
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, var version) ->
                UUID.fromString(tenantId.value());
            case TenantEvents.TenantNameUpdated(var tenantId, var _, var _, var _, var _) ->
                UUID.fromString(tenantId.value());
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    var version) -> UUID.fromString(tenantId.value());
        };
    }

    private Instant extractOccurredAt(TenantEvents event) {
        return switch (event) {
            case TenantEvents.TenantCreated(
                    var tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    var version) -> createdAt;
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, var version) -> suspendedAt;
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, var version) -> activatedAt;
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, var version) -> deactivatedAt;
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, var version) -> deletedAt;
            case TenantEvents.TenantNameUpdated(
                    var tenantId,
                    var newName,
                    var previousName,
                    var updatedAt,
                    var version) -> updatedAt;
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    var version) -> updatedAt;
        };
    }
}
