package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Students.STUDENTS;

import com.k12.common.domain.model.CourseId;
import com.k12.common.domain.model.StudentId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.models.specialization.student.GradeLevel;
import com.k12.user.domain.models.specialization.student.Student;
import com.k12.user.domain.models.specialization.student.StudentStatus;
import com.k12.user.domain.ports.out.StudentRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {

    private final AgroalDataSource dataSource;

    @Override
    @Transactional
    public Student save(Student student) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        UUID guardianIdValue =
                student.guardianId() != null ? student.guardianId().id() : null;

        ctx.insertInto(
                        STUDENTS,
                        STUDENTS.USER_ID,
                        STUDENTS.STUDENT_ID,
                        STUDENTS.GRADE_LEVEL,
                        STUDENTS.DATE_OF_BIRTH,
                        STUDENTS.GUARDIAN_ID,
                        STUDENTS.CREATED_AT,
                        STUDENTS.UPDATED_AT)
                .values(
                        student.studentId().id(),
                        student.studentNumber(),
                        student.gradeLevel(),
                        student.dateOfBirth(),
                        guardianIdValue,
                        OffsetDateTime.ofInstant(student.createdAt(), ZoneOffset.UTC),
                        OffsetDateTime.now(ZoneOffset.UTC))
                .onConflict(STUDENTS.USER_ID)
                .doUpdate()
                .set(STUDENTS.STUDENT_ID, student.studentNumber())
                .set(STUDENTS.GRADE_LEVEL, student.gradeLevel())
                .set(STUDENTS.DATE_OF_BIRTH, student.dateOfBirth())
                .set(STUDENTS.GUARDIAN_ID, guardianIdValue)
                .set(STUDENTS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();

        return student;
    }

    @Override
    public Optional<Student> findById(StudentId studentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(STUDENTS)
                .where(STUDENTS.USER_ID.eq(studentId.userId().value()))
                .fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToStudent(record));
    }

    @Override
    public Optional<Student> findByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(STUDENTS)
                .where(STUDENTS.USER_ID.eq(userId.value()))
                .fetchOne();
        return record == null ? Optional.empty() : Optional.of(mapToStudent(record));
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchExists(ctx.selectFrom(STUDENTS).where(STUDENTS.USER_ID.eq(userId.value())));
    }

    @Override
    public Set<Student> findAll() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var records = ctx.selectFrom(STUDENTS).fetch();
        Set<Student> students = new HashSet<>();
        for (var record : records) {
            students.add(mapToStudent(record));
        }
        return students;
    }

    @Override
    public Set<Student> findByGradeLevel(GradeLevel gradeLevel) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var records = ctx.selectFrom(STUDENTS)
                .where(STUDENTS.GRADE_LEVEL.eq(gradeLevel.name()))
                .fetch();

        Set<Student> students = new HashSet<>();
        for (var record : records) {
            students.add(mapToStudent(record));
        }
        return students;
    }

    @Override
    public Set<Student> findByStatus(StudentStatus status) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Note: Student status would need to be added to students table in future
        // For now, return all students
        return findAll();
    }

    @Override
    public Set<Student> findByEnrolledCourse(CourseId courseId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Note: Course enrollment would need a junction table in future
        // For now, return empty set
        return Set.of();
    }

    @Override
    public void deleteById(StudentId studentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(STUDENTS)
                .where(STUDENTS.USER_ID.eq(studentId.userId().value()))
                .execute();
    }

    @Override
    public long count() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchCount(STUDENTS);
    }

    private Student mapToStudent(org.jooq.Record record) {
        UUID guardianIdValue = record.get(STUDENTS.GUARDIAN_ID);
        ParentId guardianId = guardianIdValue != null ? new ParentId(new UserId(guardianIdValue)) : null;

        return new Student(
                new com.k12.user.domain.models.specialization.student.StudentId(
                        new UserId(record.get(STUDENTS.USER_ID))),
                record.get(STUDENTS.STUDENT_ID),
                record.get(STUDENTS.GRADE_LEVEL),
                record.get(STUDENTS.DATE_OF_BIRTH),
                guardianId,
                record.get(STUDENTS.CREATED_AT).toInstant());
    }
}
