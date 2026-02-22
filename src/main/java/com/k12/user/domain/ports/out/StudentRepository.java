package com.k12.user.domain.ports.out;

import com.k12.common.domain.model.CourseId;
import com.k12.common.domain.model.StudentId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.student.GradeLevel;
import com.k12.user.domain.models.specialization.student.Student;
import com.k12.user.domain.models.specialization.student.StudentStatus;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Student aggregates.
 */
public interface StudentRepository {

    /**
     * Saves a student aggregate.
     *
     * @param student The student to save
     * @return The saved student
     */
    Student save(Student student);

    /**
     * Finds a student by ID.
     *
     * @param studentId The student ID
     * @return Optional containing the student if found
     */
    Optional<Student> findById(StudentId studentId);

    /**
     * Finds a student by user ID.
     *
     * @param userId The user ID
     * @return Optional containing the student if found
     */
    Optional<Student> findByUserId(UserId userId);

    /**
     * Checks if a student exists by user ID.
     *
     * @param userId The user ID
     * @return true if a student exists for this user
     */
    boolean existsByUserId(UserId userId);

    /**
     * Finds all students.
     *
     * @return Set of all students
     */
    Set<Student> findAll();

    /**
     * Finds students by grade level.
     *
     * @param gradeLevel The grade level
     * @return Set of students in the grade level
     */
    Set<Student> findByGradeLevel(GradeLevel gradeLevel);

    /**
     * Finds students by status.
     *
     * @param status The student status
     * @return Set of students with the status
     */
    Set<Student> findByStatus(StudentStatus status);

    /**
     * Finds students enrolled in a specific course.
     *
     * @param courseId The course ID
     * @return Set of students enrolled in the course
     */
    Set<Student> findByEnrolledCourse(CourseId courseId);

    /**
     * Deletes a student by ID.
     *
     * @param studentId The student ID
     */
    void deleteById(StudentId studentId);

    /**
     * Counts the total number of students.
     *
     * @return The count of students
     */
    long count();
}
