package com.k12.infrastructure.repository;

import com.k12.domain.model.common.StudentId;
import com.k12.domain.model.common.UserId;
import com.k12.domain.model.specialization.parent.Parent;
import com.k12.domain.model.specialization.parent.ParentId;
import com.k12.domain.model.specialization.parent.ParentStatus;
import com.k12.domain.ports.out.ParentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;

/**
 * Stub implementation of ParentRepository.
 * TODO: Replace with actual persistence implementation.
 */
@ApplicationScoped
public class StubParentRepository implements ParentRepository {

    @Override
    public Parent save(Parent parent) {
        return parent;
    }

    @Override
    public Optional<Parent> findById(ParentId parentId) {
        return Optional.empty();
    }

    @Override
    public Optional<Parent> findByUserId(UserId userId) {
        return Optional.empty();
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        return false;
    }

    @Override
    public Set<Parent> findAll() {
        return Set.of();
    }

    @Override
    public Set<Parent> findByStatus(ParentStatus status) {
        return Set.of();
    }

    @Override
    public Set<Parent> findByLinkedStudent(StudentId studentId) {
        return Set.of();
    }

    @Override
    public void deleteById(ParentId parentId) {
        // No-op for stub
    }

    @Override
    public long count() {
        return 0;
    }
}
