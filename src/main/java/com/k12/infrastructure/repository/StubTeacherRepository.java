package com.k12.infrastructure.repository;

import com.k12.domain.model.common.CourseId;
import com.k12.domain.model.common.UserId;
import com.k12.domain.model.specialization.teacher.Teacher;
import com.k12.domain.model.specialization.teacher.TeacherId;
import com.k12.domain.ports.out.TeacherRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;

/**
 * Stub implementation of TeacherRepository.
 * TODO: Replace with actual persistence implementation.
 */
@ApplicationScoped
public class StubTeacherRepository implements TeacherRepository {

    @Override
    public Teacher save(Teacher teacher) {
        return teacher;
    }

    @Override
    public Optional<Teacher> findById(TeacherId teacherId) {
        return Optional.empty();
    }

    @Override
    public Optional<Teacher> findByUserId(UserId userId) {
        return Optional.empty();
    }

    @Override
    public Optional<Teacher> findByEmployeeId(String employeeId) {
        return Optional.empty();
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        return false;
    }

    @Override
    public Set<Teacher> findAll() {
        return Set.of();
    }

    @Override
    public Set<Teacher> findByDepartment(String department) {
        return Set.of();
    }

    @Override
    public Set<Teacher> findByAssignedCourse(CourseId courseId) {
        return Set.of();
    }

    @Override
    public void deleteById(TeacherId teacherId) {
        // No-op for stub
    }

    @Override
    public long count() {
        return 0;
    }
}
