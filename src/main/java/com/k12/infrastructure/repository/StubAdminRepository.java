package com.k12.infrastructure.repository;

import com.k12.domain.model.common.UserId;
import com.k12.domain.model.specialization.admin.Admin;
import com.k12.domain.model.specialization.admin.AdminId;
import com.k12.domain.model.specialization.admin.AdminStatus;
import com.k12.domain.ports.out.AdminRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;

/**
 * Stub implementation of AdminRepository.
 * TODO: Replace with actual persistence implementation.
 */
@ApplicationScoped
public class StubAdminRepository implements AdminRepository {

    @Override
    public Admin save(Admin admin) {
        return admin;
    }

    @Override
    public Optional<Admin> findById(AdminId adminId) {
        return Optional.empty();
    }

    @Override
    public Optional<Admin> findByUserId(UserId userId) {
        return Optional.empty();
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        return false;
    }

    @Override
    public Set<Admin> findAll() {
        return Set.of();
    }

    @Override
    public Set<Admin> findByStatus(AdminStatus status) {
        return Set.of();
    }

    @Override
    public void deleteById(AdminId adminId) {
        // No-op for stub
    }

    @Override
    public long count() {
        return 0;
    }
}
