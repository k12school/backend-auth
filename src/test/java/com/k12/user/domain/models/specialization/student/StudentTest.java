package com.k12.user.domain.models.specialization.student;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.ParentId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StudentTest {

    @Test
    void testStudentCreation() {
        UserId userId = UserId.generate();
        ParentId guardianId = ParentId.of(UserId.generate());

        Student student = StudentFactory.create(userId, "STU001", "Grade 10", LocalDate.of(2010, 5, 15), guardianId);

        assertEquals(userId, student.studentId().value());
        assertEquals("STU001", student.studentNumber());
        assertEquals("Grade 10", student.gradeLevel());
        assertEquals(LocalDate.of(2010, 5, 15), student.dateOfBirth());
        assertEquals(guardianId, student.guardianId());
        assertNotNull(student.createdAt());
    }

    @Test
    void testStudentValidation_FutureDateOfBirth() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        UserId.generate(),
                        "STU002",
                        "Grade 10",
                        LocalDate.now().plusDays(1), // future - invalid
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentValidation_NullUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        null, // invalid
                        "STU003",
                        "Grade 10",
                        LocalDate.of(2010, 5, 15),
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentValidation_NullStudentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        UserId.generate(),
                        null, // invalid
                        "Grade 10",
                        LocalDate.of(2010, 5, 15),
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentValidation_BlankStudentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        UserId.generate(),
                        "   ", // blank - invalid
                        "Grade 10",
                        LocalDate.of(2010, 5, 15),
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentValidation_NullGradeLevel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        UserId.generate(),
                        "STU004",
                        null, // invalid
                        LocalDate.of(2010, 5, 15),
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentValidation_BlankGradeLevel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        UserId.generate(),
                        "STU005",
                        "   ", // blank - invalid
                        LocalDate.of(2010, 5, 15),
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentValidation_NullDateOfBirth() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StudentFactory.create(
                        UserId.generate(),
                        "STU006",
                        "Grade 10",
                        null, // invalid
                        ParentId.of(UserId.generate())));
    }

    @Test
    void testStudentWithOptionalGuardian() {
        UserId userId = UserId.generate();

        Student student = StudentFactory.create(
                userId, "STU007", "Grade 11", LocalDate.of(2009, 3, 20), null // guardian is optional
                );

        assertEquals(userId, student.studentId().value());
        assertEquals("STU007", student.studentNumber());
        assertEquals("Grade 11", student.gradeLevel());
        assertEquals(LocalDate.of(2009, 3, 20), student.dateOfBirth());
        assertNull(student.guardianId()); // guardian can be null for orphan students
        assertNotNull(student.createdAt());
    }

    @Test
    void testStudentIdOf() {
        UserId userId = UserId.generate();
        StudentId studentId = StudentId.of(userId);

        assertEquals(userId, studentId.value());
    }

    @Test
    void testStudentIdValidation_Null() {
        assertThrows(IllegalArgumentException.class, () -> StudentId.of(null));
    }
}
