package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Parents.PARENTS;

import com.k12.common.domain.model.StudentId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.Parent;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.models.specialization.parent.ParentStatus;
import com.k12.user.domain.ports.out.ParentRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
@RequiredArgsConstructor
public class ParentRepositoryImpl implements ParentRepository {

    private final AgroalDataSource dataSource;

    @Override
    public Parent save(Parent parent) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.insertInto(
                        PARENTS,
                        PARENTS.USER_ID,
                        PARENTS.PHONE_NUMBER,
                        PARENTS.ADDRESS,
                        PARENTS.EMERGENCY_CONTACT,
                        PARENTS.CREATED_AT,
                        PARENTS.UPDATED_AT)
                .values(
                        parent.parentId().id(),
                        parent.phoneNumber(),
                        parent.address(),
                        parent.emergencyContact(),
                        OffsetDateTime.ofInstant(parent.createdAt(), ZoneOffset.UTC),
                        OffsetDateTime.now(ZoneOffset.UTC))
                .onConflict(PARENTS.USER_ID)
                .doUpdate()
                .set(PARENTS.PHONE_NUMBER, parent.phoneNumber())
                .set(PARENTS.ADDRESS, parent.address())
                .set(PARENTS.EMERGENCY_CONTACT, parent.emergencyContact())
                .set(PARENTS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();

        return parent;
    }

    @Override
    public Optional<Parent> findById(ParentId parentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record =
                ctx.selectFrom(PARENTS).where(PARENTS.USER_ID.eq(parentId.id())).fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToParent(record));
    }

    @Override
    public Optional<Parent> findByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(PARENTS)
                .where(PARENTS.USER_ID.eq(userId.value()))
                .fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToParent(record));
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchExists(ctx.selectFrom(PARENTS).where(PARENTS.USER_ID.eq(userId.value())));
    }

    @Override
    public Set<Parent> findAll() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var records = ctx.selectFrom(PARENTS).fetch();
        Set<Parent> parents = new HashSet<>();
        for (var record : records) {
            parents.add(mapToParent(record));
        }
        return parents;
    }

    @Override
    public Set<Parent> findByStatus(ParentStatus status) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Note: Parent status would need to be added to parents table in future
        // For now, return all parents
        return findAll();
    }

    @Override
    public Set<Parent> findByLinkedStudent(StudentId studentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Find parents linked to students via guardian_id
        var records = ctx.selectFrom(PARENTS)
                .where(PARENTS.USER_ID.in(
                        ctx.select(com.k12.backend.infrastructure.jooq.public_.tables.Students.STUDENTS.GUARDIAN_ID)
                                .from(com.k12.backend.infrastructure.jooq.public_.tables.Students.STUDENTS)
                                .where(com.k12.backend.infrastructure.jooq.public_.tables.Students.STUDENTS.USER_ID.eq(
                                        studentId.userId().value()))))
                .fetch();

        Set<Parent> parents = new HashSet<>();
        for (var record : records) {
            parents.add(mapToParent(record));
        }
        return parents;
    }

    @Override
    public void deleteById(ParentId parentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(PARENTS).where(PARENTS.USER_ID.eq(parentId.id())).execute();
    }

    @Override
    public long count() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchCount(PARENTS);
    }

    private Parent mapToParent(org.jooq.Record record) {
        return new Parent(
                new com.k12.user.domain.models.specialization.parent.ParentId(new UserId(record.get(PARENTS.USER_ID))),
                record.get(PARENTS.PHONE_NUMBER),
                record.get(PARENTS.ADDRESS),
                record.get(PARENTS.EMERGENCY_CONTACT),
                record.get(PARENTS.CREATED_AT).toInstant());
    }
}
