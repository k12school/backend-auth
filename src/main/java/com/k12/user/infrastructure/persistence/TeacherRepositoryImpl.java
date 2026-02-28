package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Teachers.TEACHERS;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherId;
import com.k12.user.domain.ports.out.TeacherRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
@RequiredArgsConstructor
public class TeacherRepositoryImpl implements TeacherRepository {

    private final AgroalDataSource dataSource;

    @Override
    public Teacher save(Teacher teacher) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.insertInto(
                        TEACHERS,
                        TEACHERS.USER_ID,
                        TEACHERS.EMPLOYEE_ID,
                        TEACHERS.DEPARTMENT,
                        TEACHERS.HIRE_DATE,
                        TEACHERS.CREATED_AT,
                        TEACHERS.UPDATED_AT)
                .values(
                        teacher.teacherId().value().value(),
                        teacher.employeeId(),
                        teacher.department(),
                        teacher.hireDate(),
                        OffsetDateTime.ofInstant(teacher.createdAt(), ZoneOffset.UTC),
                        OffsetDateTime.now(ZoneOffset.UTC))
                .onConflict(TEACHERS.USER_ID)
                .doUpdate()
                .set(TEACHERS.EMPLOYEE_ID, teacher.employeeId())
                .set(TEACHERS.DEPARTMENT, teacher.department())
                .set(TEACHERS.HIRE_DATE, teacher.hireDate())
                .set(TEACHERS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();

        return teacher;
    }

    @Override
    public Optional<Teacher> findById(TeacherId teacherId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(TEACHERS)
                .where(TEACHERS.USER_ID.eq(teacherId.value().value()))
                .fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToTeacher(record));
    }

    @Override
    public Optional<Teacher> findByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(TEACHERS)
                .where(TEACHERS.USER_ID.eq(userId.value()))
                .fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToTeacher(record));
    }

    @Override
    public Optional<Teacher> findByEmployeeId(String employeeId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(TEACHERS)
                .where(TEACHERS.EMPLOYEE_ID.eq(employeeId))
                .fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToTeacher(record));
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchExists(ctx.selectFrom(TEACHERS).where(TEACHERS.USER_ID.eq(userId.value())));
    }

    @Override
    public java.util.Set<Teacher> findAll() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var records = ctx.selectFrom(TEACHERS).fetch();
        java.util.Set<Teacher> teachers = new java.util.HashSet<>();
        for (var record : records) {
            teachers.add(mapToTeacher(record));
        }
        return teachers;
    }

    @Override
    public java.util.Set<Teacher> findByDepartment(String department) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var records = ctx.selectFrom(TEACHERS)
                .where(TEACHERS.DEPARTMENT.eq(department))
                .fetch();
        java.util.Set<Teacher> teachers = new java.util.HashSet<>();
        for (var record : records) {
            teachers.add(mapToTeacher(record));
        }
        return teachers;
    }

    @Override
    public java.util.Set<Teacher> findByAssignedCourse(com.k12.common.domain.model.CourseId courseId) {
        // TODO: Implement course assignment logic when course relationship is defined
        return new java.util.HashSet<>();
    }

    @Override
    public void deleteById(TeacherId teacherId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(TEACHERS)
                .where(TEACHERS.USER_ID.eq(teacherId.value().value()))
                .execute();
    }

    @Override
    public long count() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchCount(TEACHERS);
    }

    private Teacher mapToTeacher(org.jooq.Record record) {
        return new Teacher(
                TeacherId.of(record.get(TEACHERS.USER_ID, UserId.class)),
                record.get(TEACHERS.EMPLOYEE_ID),
                record.get(TEACHERS.DEPARTMENT),
                record.get(TEACHERS.HIRE_DATE, LocalDate.class),
                record.get(TEACHERS.CREATED_AT, OffsetDateTime.class).toInstant());
    }
}
