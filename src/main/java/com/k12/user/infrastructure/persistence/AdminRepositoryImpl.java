package com.k12.user.infrastructure.persistence;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.AdminId;
import com.k12.user.domain.models.specialization.admin.AdminStatus;
import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import com.k12.user.domain.ports.out.AdminRepository;
import io.agroal.api.AgroalDataSource;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Implementation of AdminRepository using jOOQ.
 * Admin is a specialization of User with additional permissions.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminRepository {

    private final AgroalDataSource dataSource;
    private final Tracer tracer;

    private static final String ADMINS_TABLE = "admins";

    @Override
    @Transactional
    public Admin save(Admin admin) {
        Span span = tracer.spanBuilder("AdminRepository.save")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

            String permissions =
                    admin.permissions().stream().map(Permission::name).collect(Collectors.joining(","));

            OffsetDateTime createdAt = OffsetDateTime.ofInstant(admin.createdAt(), ZoneOffset.UTC);

            ctx.insertInto(
                            DSL.table(ADMINS_TABLE),
                            DSL.field("user_id"),
                            DSL.field("permissions"),
                            DSL.field("status"),
                            DSL.field("created_at"))
                    .values(admin.adminId().userId().value(), permissions, "ACTIVE", createdAt)
                    .onConflict(DSL.field("user_id"))
                    .doUpdate()
                    .set(DSL.field("permissions"), permissions)
                    .set(DSL.field("updated_at"), OffsetDateTime.now())
                    .execute();

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return admin;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw new RuntimeException("Failed to save admin", e);
        } finally {
            span.end();
        }
    }

    @Override
    public Optional<Admin> findById(AdminId adminId) {
        return findByUserId(adminId.userId());
    }

    @Override
    public Optional<Admin> findByUserId(UserId userId) {
        Span span = tracer.spanBuilder("AdminRepository.findByUserId")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

            var record = ctx.selectFrom(DSL.table(ADMINS_TABLE))
                    .where(DSL.field("user_id").eq(userId.value()))
                    .fetchOne();

            if (record == null) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                return Optional.empty();
            }

            Admin admin = mapToAdmin(userId, record);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return Optional.of(admin);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            return Optional.empty();
        } finally {
            span.end();
        }
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        return findByUserId(userId).isPresent();
    }

    @Override
    public Set<Admin> findAll() {
        Span span = tracer.spanBuilder("AdminRepository.findAll")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

            var records = ctx.selectFrom(DSL.table(ADMINS_TABLE)).fetch();

            Set<Admin> admins = records.stream()
                    .map(record -> {
                        UUID userId = record.get(DSL.field("user_id", UUID.class));
                        return mapToAdmin(new UserId(userId), record);
                    })
                    .collect(Collectors.toSet());

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return admins;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            return Set.of();
        } finally {
            span.end();
        }
    }

    @Override
    public Set<Admin> findByStatus(AdminStatus status) {
        Span span = tracer.spanBuilder("AdminRepository.findByStatus")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

            var records = ctx.selectFrom(DSL.table(ADMINS_TABLE))
                    .where(DSL.field("status").eq(status.name()))
                    .fetch();

            Set<Admin> admins = records.stream()
                    .map(record -> {
                        UUID userId = record.get(DSL.field("user_id", UUID.class));
                        return mapToAdmin(new UserId(userId), record);
                    })
                    .collect(Collectors.toSet());

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return admins;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            return Set.of();
        } finally {
            span.end();
        }
    }

    @Override
    @Transactional
    public void deleteById(AdminId adminId) {
        Span span = tracer.spanBuilder("AdminRepository.deleteById")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

            ctx.deleteFrom(DSL.table(ADMINS_TABLE))
                    .where(DSL.field("user_id").eq(adminId.userId().value()))
                    .execute();

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw new RuntimeException("Failed to delete admin", e);
        } finally {
            span.end();
        }
    }

    @Override
    public long count() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchCount(DSL.table(ADMINS_TABLE));
    }

    /**
     * Maps a database record to an Admin aggregate.
     */
    private Admin mapToAdmin(UserId userId, org.jooq.Record record) {
        String permissionsStr = record.get(DSL.field("permissions", String.class));
        Instant createdAt =
                record.get(DSL.field("created_at", OffsetDateTime.class)).toInstant();

        Set<Permission> permissions = parsePermissions(permissionsStr);

        return new Admin(AdminId.of(userId), permissions, createdAt);
    }

    /**
     * Parses comma-separated permissions string to Set of Permission enums.
     */
    private Set<Permission> parsePermissions(String permissionsStr) {
        if (permissionsStr == null || permissionsStr.isBlank()) {
            return Set.of();
        }

        Set<Permission> permissions = new LinkedHashSet<>();
        for (String perm : permissionsStr.split(",")) {
            try {
                permissions.add(Permission.valueOf(perm.trim()));
            } catch (IllegalArgumentException e) {
                // Skip invalid permission values
                continue;
            }
        }
        return permissions;
    }
}
