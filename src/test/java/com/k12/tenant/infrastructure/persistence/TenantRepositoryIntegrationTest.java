package com.k12.tenant.infrastructure.persistence;

import static com.k12.tenant.domain.models.error.TenantError.ConcurrencyError.VERSION_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.TenantFactory;
import com.k12.tenant.domain.models.commands.TenantCommands;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.domain.models.events.TenantEvents;
import com.k12.tenant.domain.port.TenantRepository;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Integration tests for TenantRepository demonstrating concurrency and atomicity. */
@QuarkusTest
@DisplayName("TenantRepository Integration Tests")
class TenantRepositoryIntegrationTest {

    @Inject
    TenantRepository tenantRepository;

    @Inject
    AgroalDataSource dataSource;

    private DSLContext ctx;

    @BeforeEach
    void setUp() {
        ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(com.k12.backend.infrastructure.jooq.public_.tables.TenantEvents.TENANT_EVENTS)
                .execute();
        ctx.deleteFrom(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS)
                .execute();
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) {
            ctx.deleteFrom(com.k12.backend.infrastructure.jooq.public_.tables.TenantEvents.TENANT_EVENTS)
                    .execute();
            ctx.deleteFrom(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS)
                    .execute();
        }
    }

    @Nested
    @DisplayName("Concurrency Conflict Detection")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should detect version conflict when two writers append same version")
        void testConcurrentAppendSameVersion() throws Exception {
            var createCommand = new TenantCommands.CreateTenant("Test Tenant", "test");
            var eventResult = TenantFactory.handle(createCommand);
            assertThat(eventResult.isSuccess()).isTrue();
            TenantEvents createdEvent = eventResult.get();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CompletableFuture<Result<Void, TenantError>> future1 =
                    CompletableFuture.supplyAsync(() -> tenantRepository.append(createdEvent, 1L), executor);
            CompletableFuture<Result<Void, TenantError>> future2 =
                    CompletableFuture.supplyAsync(() -> tenantRepository.append(createdEvent, 1L), executor);

            CompletableFuture.allOf(future1, future2).get(10, TimeUnit.SECONDS);
            executor.shutdown();

            Result<Void, TenantError> result1 = future1.get();
            Result<Void, TenantError> result2 = future2.get();

            boolean oneSuccess = result1.isSuccess() ^ result2.isSuccess();
            boolean oneConflict = (result1.isFailure() && result1.getError() == VERSION_CONFLICT)
                    || (result2.isFailure() && result2.getError() == VERSION_CONFLICT);

            assertThat(oneSuccess).isTrue();
            assertThat(oneConflict).isTrue();
        }

        @Test
        @DisplayName("Should enforce sequential version numbers")
        void testSequentialVersionEnforcement() {
            var createCommand = new TenantCommands.CreateTenant("Test Tenant", "test");
            var createdResult = TenantFactory.handle(createCommand);
            assertThat(createdResult.isSuccess()).isTrue();
            TenantEvents createdEvent = createdResult.get();

            Result<Void, TenantError> firstAppend = tenantRepository.append(createdEvent, 1L);
            assertThat(firstAppend.isSuccess()).isTrue();

            Result<Void, TenantError> secondAppend = tenantRepository.append(createdEvent, 1L);
            assertThat(secondAppend.isFailure()).isTrue();
            assertThat(secondAppend.getError()).isEqualTo(VERSION_CONFLICT);
        }
    }

    @Nested
    @DisplayName("Atomicity Tests")
    class AtomicityTests {

        @Test
        @DisplayName("Should atomically append event and update projection")
        void testAtomicityOnEventInsert() {
            var createCommand = new TenantCommands.CreateTenant("Test Tenant", "test");
            var createdResult = TenantFactory.handle(createCommand);
            assertThat(createdResult.isSuccess()).isTrue();
            TenantEvents createdEvent = createdResult.get();
            TenantId tenantId = extractTenantId(createdEvent);

            Result<Void, TenantError> appendResult = tenantRepository.append(createdEvent, 1L);
            assertThat(appendResult.isSuccess()).isTrue();

            Result<List<TenantEvents>, TenantError> eventsResult = tenantRepository.loadEvents(tenantId);
            assertThat(eventsResult.isSuccess()).isTrue();
            assertThat(eventsResult.get()).hasSize(1);

            boolean projectionExists = ctx.fetchExists(ctx.selectOne()
                    .from(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS)
                    .where(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS.ID.eq(
                            java.util.UUID.fromString(tenantId.value()))));
            assertThat(projectionExists).isTrue();
        }

        @Test
        @DisplayName("Should not update projection if version conflict occurs")
        void testProjectionNotUpdatedOnVersionConflict() {
            var createCommand = new TenantCommands.CreateTenant("Test Tenant", "test");
            var createdResult = TenantFactory.handle(createCommand);
            assertThat(createdResult.isSuccess()).isTrue();
            TenantEvents createdEvent = createdResult.get();
            TenantId tenantId = extractTenantId(createdEvent);

            Result<Void, TenantError> firstAppend = tenantRepository.append(createdEvent, 1L);
            assertThat(firstAppend.isSuccess()).isTrue();

            var projectionBefore = ctx.selectFrom(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS)
                    .where(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS.ID.eq(
                            java.util.UUID.fromString(tenantId.value())))
                    .fetchOne();

            Result<Void, TenantError> secondAppend = tenantRepository.append(createdEvent, 1L);
            assertThat(secondAppend.isFailure()).isTrue();
            assertThat(secondAppend.getError()).isEqualTo(VERSION_CONFLICT);

            var projectionAfter = ctx.selectFrom(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS)
                    .where(com.k12.backend.infrastructure.jooq.public_.tables.Tenants.TENANTS.ID.eq(
                            java.util.UUID.fromString(tenantId.value())))
                    .fetchOne();

            assertThat(projectionAfter.getVersion()).isEqualTo(projectionBefore.getVersion());
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
}
