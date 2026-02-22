package com.k12.infrastructure.repository;

import com.k12.domain.model.common.UserId;
import com.k12.domain.model.user.User;
import com.k12.domain.ports.out.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Stub implementation of UserRepository.
 * TODO: Replace with actual persistence implementation.
 */
@ApplicationScoped
public class StubUserRepository implements UserRepository {

    @Override
    public User save(User user) {
        return user;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmailAddress(String emailAddress) {
        return Optional.empty();
    }

    @Override
    public boolean existsByEmail(String emailAddress) {
        return false;
    }

    @Override
    public void deleteById(UserId userId) {
        // No-op for stub
    }

    @Override
    public long count() {
        return 0;
    }
}
