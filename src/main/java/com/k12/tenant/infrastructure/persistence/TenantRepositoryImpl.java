package com.k12.tenant.infrastructure.persistence;

import static com.k12.tenant.domain.models.error.TenantError.ConcurrencyError.VERSION_CONFLICT;
import static com.k12.tenant.domain.models.error.TenantError.PersistenceError.STORAGE_ERROR;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Tenant;
import com.k12.tenant.domain.models.TenantReconstructor;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import com.k12.tenant.domain.port.TenantRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Implementation of TenantRepository using jOOQ DSL for event sourcing.
 * All methods return Result<T, E> following ROP pattern.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final AgroalDataSource dataSource;

    // Table and column names (jOOQ will generate these from schema)
    private static final String TENANT_EVENTS = "tenant_events";
    private static final String TENANTS = "tenants";

    @Override
    public Result<Void, TenantError> append(TenantEvents event, long expectedVersion) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        TenantId tenantId = extractTenantId(event);

        try {
            // Check for version conflict (optimistic locking)
            Long currentVersion = ctx.select(DSL.field("version"))
                    .from(TENANT_EVENTS)
                    .where(DSL.field("tenant_id").eq(UUID.fromString(tenantId.value())))
                    .orderBy(DSL.field("version").desc())
                    .fetchOne(DSL.field("version", Long.class));

            if (currentVersion != null && currentVersion >= expectedVersion) {
                return Result.failure(VERSION_CONFLICT);
            }

            // Serialize event to binary using Kryo
            byte[] eventData = KryoEventSerializer.serialize(event);
            String eventType = event.getClass().getSimpleName();

            // Insert event
            ctx.insertInto(
                            DSL.table(TENANT_EVENTS),
                            DSL.field("tenant_id"),
                            DSL.field("event_type"),
                            DSL.field("event_data"),
                            DSL.field("version"),
                            DSL.field("occurred_at"))
                    .values(
                            UUID.fromString(tenantId.value()),
                            eventType,
                            eventData,
                            expectedVersion,
                            extractOccurredAt(event))
                    .execute();

            // Update projection asynchronously (synchronously for now)
            updateProjection(event);

            return Result.success(null);
        } catch (Exception e) {
            // Handle unique constraint violation (version conflict)
            if (e.getCause() instanceof SQLException) {
                return Result.failure(VERSION_CONFLICT);
            }
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    public Result<List<TenantEvents>, TenantError> loadEvents(TenantId tenantId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            List<Record> records = ctx.selectFrom(TENANT_EVENTS)
                    .where(DSL.field("tenant_id").eq(UUID.fromString(tenantId.value())))
                    .orderBy(DSL.field("version").asc())
                    .fetch();

            List<TenantEvents> events = new ArrayList<>();
            for (Record record : records) {
                byte[] eventData = record.get("event_data", byte[].class);
                TenantEvents event = KryoEventSerializer.deserialize(eventData);
                events.add(event);
            }

            return Result.success(events);
        } catch (Exception e) {
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
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
            Long version = ctx.select(DSL.max(DSL.field("version", Long.class)))
                    .from(TENANT_EVENTS)
                    .where(DSL.field("tenant_id").eq(UUID.fromString(tenantId.value())))
                    .fetchOneInto(Long.class);

            if (version == null) {
                return Result.failure(TenantError.ValidationError.VALUE_REQUIRED);
            }

            return Result.success(version);
        } catch (Exception e) {
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    public Result<Boolean, TenantError> nameExists(String name) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            Integer count = ctx.select(DSL.count())
                    .from(TENANTS)
                    .where(DSL.field("name").eq(name))
                    .fetchOneInto(Integer.class);

            return Result.success(count != null && count > 0);
        } catch (Exception e) {
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    public Result<Boolean, TenantError> subdomainExists(String subdomain) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            Integer count = ctx.select(DSL.count())
                    .from(TENANTS)
                    .where(DSL.field("subdomain").eq(subdomain))
                    .fetchOneInto(Integer.class);

            return Result.success(count != null && count > 0);
        } catch (Exception e) {
            return Result.failure(STORAGE_ERROR);
        }
    }

    @Override
    public Optional<TenantId> findByName(String name) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            UUID id = ctx.select(DSL.field("id", UUID.class))
                    .from(TENANTS)
                    .where(DSL.field("name").eq(name))
                    .fetchOneInto(UUID.class);

            return id != null ? Optional.of(new TenantId(id.toString())) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantId> findBySubdomain(String subdomain) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            UUID id = ctx.select(DSL.field("id", UUID.class))
                    .from(TENANTS)
                    .where(DSL.field("subdomain").eq(subdomain))
                    .fetchOneInto(UUID.class);

            return id != null ? Optional.of(new TenantId(id.toString())) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Updates the read projection after appending an event.
     */
    private void updateProjection(TenantEvents event) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        switch (event) {
            case TenantEvents.TenantCreated(
                    TenantId tenantId,
                    var name,
                    var subdomain,
                    var status,
                    var createdAt,
                    var version) -> {
                ctx.insertInto(
                                DSL.table(TENANTS),
                                DSL.field("id"),
                                DSL.field("name"),
                                DSL.field("subdomain"),
                                DSL.field("status"),
                                DSL.field("version"),
                                DSL.field("created_at"),
                                DSL.field("updated_at"))
                        .values(
                                UUID.fromString(tenantId.value()),
                                name.value(),
                                subdomain.value(),
                                status.name(),
                                version,
                                createdAt,
                                createdAt)
                        .onConflict(DSL.field("id"))
                        .doNothing()
                        .execute();
                break;
            }
            case TenantEvents.TenantSuspended(var tenantId, var suspendedAt, var version) -> {
                ctx.update(DSL.table(TENANTS))
                        .set(DSL.field("status"), "SUSPENDED")
                        .set(DSL.field("version"), version)
                        .set(DSL.field("updated_at"), suspendedAt)
                        .where(DSL.field("id").eq(UUID.fromString(tenantId.value())))
                        .execute();
                break;
            }
            case TenantEvents.TenantActivated(var tenantId, var activatedAt, var version) -> {
                ctx.update(DSL.table(TENANTS))
                        .set(DSL.field("status"), "ACTIVE")
                        .set(DSL.field("version"), version)
                        .set(DSL.field("updated_at"), activatedAt)
                        .where(DSL.field("id").eq(UUID.fromString(tenantId.value())))
                        .execute();
                break;
            }
            case TenantEvents.TenantDeactivated(var tenantId, var deactivatedAt, var version) -> {
                ctx.update(DSL.table(TENANTS))
                        .set(DSL.field("status"), "INACTIVE")
                        .set(DSL.field("version"), version)
                        .set(DSL.field("updated_at"), deactivatedAt)
                        .where(DSL.field("id").eq(UUID.fromString(tenantId.value())))
                        .execute();
                break;
            }
            case TenantEvents.TenantDeleted(var tenantId, var deletedAt, var version) -> {
                ctx.deleteFrom(DSL.table(TENANTS))
                        .where(DSL.field("id").eq(UUID.fromString(tenantId.value())))
                        .execute();
                break;
            }
            case TenantEvents.TenantNameUpdated(
                    var tenantId,
                    var newName,
                    var previousName,
                    var updatedAt,
                    var version) -> {
                ctx.update(DSL.table(TENANTS))
                        .set(DSL.field("name"), newName.value())
                        .set(DSL.field("version"), version)
                        .set(DSL.field("updated_at"), updatedAt)
                        .where(DSL.field("id").eq(UUID.fromString(tenantId.value())))
                        .execute();
                break;
            }
            case TenantEvents.TenantSubdomainUpdated(
                    var tenantId,
                    var newSubdomain,
                    var previousSubdomain,
                    var updatedAt,
                    var version) -> {
                ctx.update(DSL.table(TENANTS))
                        .set(DSL.field("subdomain"), newSubdomain.value())
                        .set(DSL.field("version"), version)
                        .set(DSL.field("updated_at"), updatedAt)
                        .where(DSL.field("id").eq(UUID.fromString(tenantId.value())))
                        .execute();
                break;
            }
        }
    }

    private TenantId extractTenantId(TenantEvents event) {
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

    private java.time.Instant extractOccurredAt(TenantEvents event) {
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
