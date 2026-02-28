package com.k12.user.domain.ports.out;

import com.k12.common.domain.model.CourseId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherId;
import java.util.Optional;
import java.util.Set;
import org.jooq.DSLContext;

/**
 * Repository interface for Teacher aggregates.
 */
public interface TeacherRepository {

    /**
     * Saves a teacher aggregate.
     *
     * @param teacher The teacher to save
     * @return The saved teacher
     */
    Teacher save(Teacher teacher);

    /**
     * Saves a teacher aggregate using a specific DSLContext.
     * This overload supports multi-repository transactions.
     *
     * @param teacher the teacher to save
     * @param ctx the DSLContext to use (must be transactional)
     * @return the saved teacher
     */
    Teacher save(Teacher teacher, DSLContext ctx);

    /**
     * Finds a teacher by ID.
     *
     * @param teacherId The teacher ID
     * @return Optional containing the teacher if found
     */
    Optional<Teacher> findById(TeacherId teacherId);

    /**
     * Finds a teacher by user ID.
     *
     * @param userId The user ID
     * @return Optional containing the teacher if found
     */
    Optional<Teacher> findByUserId(UserId userId);

    /**
     * Finds a teacher by employee ID.
     *
     * @param employeeId The employee ID
     * @return Optional containing the teacher if found
     */
    Optional<Teacher> findByEmployeeId(String employeeId);

    /**
     * Checks if a teacher exists by user ID.
     *
     * @param userId The user ID
     * @return true if a teacher exists for this user
     */
    boolean existsByUserId(UserId userId);

    /**
     * Finds all teachers.
     *
     * @return Set of all teachers
     */
    Set<Teacher> findAll();

    /**
     * Finds teachers by department.
     *
     * @param department The department name
     * @return Set of teachers in the department
     */
    Set<Teacher> findByDepartment(String department);

    /**
     * Finds teachers assigned to a specific course.
     *
     * @param courseId The course ID
     * @return Set of teachers assigned to the course
     */
    Set<Teacher> findByAssignedCourse(CourseId courseId);

    /**
     * Deletes a teacher by ID.
     *
     * @param teacherId The teacher ID
     */
    void deleteById(TeacherId teacherId);

    /**
     * Counts the total number of teachers.
     *
     * @return The count of teachers
     */
    long count();
}
