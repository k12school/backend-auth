package com.k12.infrastructure.repository;

import com.k12.domain.model.common.CourseId;
import com.k12.domain.model.common.StudentId;
import com.k12.domain.model.common.UserId;
import com.k12.domain.model.specialization.student.GradeLevel;
import com.k12.domain.model.specialization.student.Student;
import com.k12.domain.model.specialization.student.StudentStatus;
import com.k12.domain.ports.out.StudentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;

/**
 * Stub implementation of StudentRepository.
 * TODO: Replace with actual persistence implementation.
 */
@ApplicationScoped
public class StubStudentRepository implements StudentRepository {

    @Override
    public Student save(Student student) {
        return student;
    }

    @Override
    public Optional<Student> findById(StudentId studentId) {
        return Optional.empty();
    }

    @Override
    public Optional<Student> findByUserId(UserId userId) {
        return Optional.empty();
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        return false;
    }

    @Override
    public Set<Student> findAll() {
        return Set.of();
    }

    @Override
    public Set<Student> findByGradeLevel(GradeLevel gradeLevel) {
        return Set.of();
    }

    @Override
    public Set<Student> findByStatus(StudentStatus status) {
        return Set.of();
    }

    @Override
    public Set<Student> findByEnrolledCourse(CourseId courseId) {
        return Set.of();
    }

    @Override
    public void deleteById(StudentId studentId) {
        // No-op for stub
    }

    @Override
    public long count() {
        return 0;
    }
}
