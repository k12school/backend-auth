package com.k12.user.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherFactory;
import com.k12.user.domain.ports.out.TeacherRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TeacherRepositoryImplTest {

    @Inject
    TeacherRepository teacherRepository;

    @Inject
    io.agroal.api.AgroalDataSource dataSource;

    private DSLContext getDSLContext() {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    private void createTestUser(UUID userId) {
        DSLContext ctx = getDSLContext();

        ctx.insertInto(com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS)
                .columns(
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ID,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.EMAIL,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.PASSWORD_HASH,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ROLES,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.STATUS,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.NAME,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.CREATED_AT,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.UPDATED_AT,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.TENANT_ID)
                .values(
                        userId,
                        "teacher" + userId + "@test.com",
                        "dummy-hash",
                        "TEACHER",
                        "ACTIVE",
                        "Test Teacher",
                        java.time.OffsetDateTime.now(),
                        java.time.OffsetDateTime.now(),
                        UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
                .execute();
    }

    @Test
    void testSaveAndFindTeacher() {
        // Create a user entry directly in the database to bypass Kryo serialization
        UUID userId = UUID.randomUUID();
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        // Insert user directly (simplified, minimal data)
        ctx.insertInto(com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS)
                .columns(
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ID,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.EMAIL,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.PASSWORD_HASH,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ROLES,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.STATUS,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.NAME,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.CREATED_AT,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.UPDATED_AT,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.TENANT_ID)
                .values(
                        userId,
                        "teacher@test.com",
                        "dummy-hash",
                        "TEACHER",
                        "ACTIVE",
                        "Test Teacher",
                        java.time.OffsetDateTime.now(),
                        java.time.OffsetDateTime.now(),
                        UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
                .execute();

        UserId userIdObj = UserId.of(userId.toString());
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP123", "Science", LocalDate.of(2024, 1, 15));

        Teacher saved = teacherRepository.save(teacher);

        var found = teacherRepository.findByUserId(userIdObj);
        assertTrue(found.isPresent());
        assertEquals("EMP123", found.get().employeeId());
    }

    @Test
    void testFindById() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440001");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP456", "Mathematics", LocalDate.of(2024, 2, 1));

        teacherRepository.save(teacher);

        var found = teacherRepository.findById(teacher.teacherId());
        assertTrue(found.isPresent());
        assertEquals("EMP456", found.get().employeeId());
        assertEquals("Mathematics", found.get().department());
    }

    @Test
    void testFindByEmployeeId() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440002");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP789", "English", LocalDate.of(2024, 3, 1));

        teacherRepository.save(teacher);

        var found = teacherRepository.findByEmployeeId("EMP789");
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().teacherId().value());
        assertEquals("English", found.get().department());
    }

    @Test
    void testExistsByUserId() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440003");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP321", "History", LocalDate.of(2024, 4, 1));

        assertFalse(teacherRepository.existsByUserId(userIdObj));

        teacherRepository.save(teacher);

        assertTrue(teacherRepository.existsByUserId(userIdObj));
    }

    @Test
    void testEmployeeUniqueness() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440010");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP654", "Geography", LocalDate.of(2024, 5, 1));

        assertFalse(teacherRepository.findByEmployeeId("EMP654").isPresent());

        teacherRepository.save(teacher);

        assertTrue(teacherRepository.findByEmployeeId("EMP654").isPresent());
    }

    @Test
    void testFindAll() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        createTestUser(userId1);
        createTestUser(userId2);

        UserId userId1Obj = UserId.of("550e8400-e29b-41d4-a716-446655440004");
        UserId userId2Obj = UserId.of("550e8400-e29b-41d4-a716-446655440005");

        int initialCount = teacherRepository.findAll().size();

        Teacher teacher1 = TeacherFactory.create(userId1Obj, "EMP100", "Physics", LocalDate.of(2024, 1, 1));
        Teacher teacher2 = TeacherFactory.create(userId2Obj, "EMP200", "Chemistry", LocalDate.of(2024, 1, 2));

        teacherRepository.save(teacher1);
        teacherRepository.save(teacher2);

        Set<Teacher> allTeachers = teacherRepository.findAll();
        assertEquals(initialCount + 2, allTeachers.size());

        assertTrue(allTeachers.stream().anyMatch(t -> t.employeeId().equals("EMP100")));
        assertTrue(allTeachers.stream().anyMatch(t -> t.employeeId().equals("EMP200")));
    }

    @Test
    void testFindByDepartment() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440006");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP300", "Computer Science", LocalDate.of(2024, 6, 1));

        teacherRepository.save(teacher);

        Set<Teacher> csTeachers = teacherRepository.findByDepartment("Computer Science");
        assertEquals(1, csTeachers.size());
        assertEquals("EMP300", csTeachers.iterator().next().employeeId());

        Set<Teacher> mathTeachers = teacherRepository.findByDepartment("Mathematics");
        assertTrue(mathTeachers.isEmpty());
    }

    @Test
    void testDeleteById() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440007");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP400", "Biology", LocalDate.of(2024, 7, 1));

        teacherRepository.save(teacher);

        var found = teacherRepository.findById(teacher.teacherId());
        assertTrue(found.isPresent());

        teacherRepository.deleteById(teacher.teacherId());

        var afterDelete = teacherRepository.findById(teacher.teacherId());
        assertTrue(afterDelete.isEmpty());
    }

    @Test
    void testCount() {
        long initialCount = teacherRepository.count();

        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440008");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP500", "Art", LocalDate.of(2024, 8, 1));

        teacherRepository.save(teacher);

        assertEquals(initialCount + 1, teacherRepository.count());
    }

    @Test
    void testUpdateTeacher() {
        UUID userId = UUID.randomUUID();
        createTestUser(userId);
        UserId userIdObj = UserId.of("550e8400-e29b-41d4-a716-446655440009");
        Teacher teacher = TeacherFactory.create(userIdObj, "EMP600", "Music", LocalDate.of(2024, 9, 1));

        teacherRepository.save(teacher);

        var found = teacherRepository.findByUserId(userIdObj);
        assertTrue(found.isPresent());
        assertEquals("Music", found.get().department());

        // Update the teacher by creating a new instance with same ID but different data
        Teacher updatedTeacher =
                TeacherFactory.create(userIdObj, "EMP600", "Physical Education", LocalDate.of(2024, 9, 15));
        teacherRepository.save(updatedTeacher);

        var updatedFound = teacherRepository.findByUserId(userIdObj);
        assertTrue(updatedFound.isPresent());
        assertEquals("Physical Education", updatedFound.get().department());
    }
}
