package com.k12.user.domain.models.specialization.teacher;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.UserId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TeacherTest {

    @Test
    void testTeacherCreation() {
        UserId userId = UserId.generate();
        Teacher teacher = TeacherFactory.create(userId, "EMP001", "Mathematics", LocalDate.of(2024, 1, 15));

        assertEquals(userId, teacher.teacherId().value());
        assertEquals("EMP001", teacher.employeeId());
        assertEquals("Mathematics", teacher.department());
        assertEquals(LocalDate.of(2024, 1, 15), teacher.hireDate());
        assertNotNull(teacher.createdAt());
    }

    @Test
    void testTeacherValidation_EmptyEmployeeId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TeacherFactory.create(
                        UserId.generate(),
                        "", // invalid
                        "Mathematics",
                        LocalDate.now()));
    }

    @Test
    void testTeacherValidation_NullEmployeeId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TeacherFactory.create(
                        UserId.generate(),
                        null, // invalid
                        "Mathematics",
                        LocalDate.now()));
    }

    @Test
    void testTeacherValidation_NullTeacherId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Teacher(
                        null, // invalid
                        "EMP001",
                        "Mathematics",
                        LocalDate.now(),
                        null));
    }

    @Test
    void testTeacherWithDefaults() {
        UserId userId = UserId.generate();
        Teacher teacher = TeacherFactory.create(
                userId,
                "EMP002",
                null, // department can be null
                null // hireDate defaults to now
                );

        assertEquals(userId, teacher.teacherId().value());
        assertEquals("EMP002", teacher.employeeId());
        assertNull(teacher.department());
        assertNotNull(teacher.hireDate());
        assertNotNull(teacher.createdAt());
    }

    @Test
    void testTeacherIdOf() {
        UserId userId = UserId.generate();
        TeacherId teacherId = TeacherId.of(userId);

        assertEquals(userId, teacherId.value());
        assertEquals(userId.value(), teacherId.id());
    }

    @Test
    void testTeacherIdValidation_Null() {
        assertThrows(IllegalArgumentException.class, () -> TeacherId.of(null));
    }
}
