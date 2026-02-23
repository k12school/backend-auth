package com.k12.user.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.ports.out.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserRepositoryImplIntegrationTest {
    @Inject
    UserRepository userRepository;

    @Inject
    SetupTestData setupTestData;

    @Test
    void shouldFindUserByEmail() {
        // Ensure test data exists
        setupTestData.setupTestUser();

        Optional<User> user = userRepository.findByEmailAddress("admin@k12.com");
        assertTrue(user.isPresent());
        assertEquals("admin@k12.com", user.get().emailAddress().value());
        assertTrue(user.get().userRole().contains(UserRole.SUPER_ADMIN));
    }

    @Test
    void shouldReturnEmptyForNonExistentEmail() {
        Optional<User> user = userRepository.findByEmailAddress("nonexistent@example.com");
        assertTrue(user.isEmpty());
    }

    @Test
    void shouldFindUserById() {
        // Ensure test data exists
        setupTestData.setupTestUser();

        UserId userId = UserId.of("550e8400-e29b-41d4-a716-446655440000");
        Optional<User> user = userRepository.findById(userId);
        assertTrue(user.isPresent());
        assertEquals(userId, user.get().userId());
    }
}
