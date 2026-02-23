package com.k12.user.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.*;
import com.k12.user.domain.ports.out.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {
    private UserRepository userRepository;
    private AuthenticationService authenticationService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authenticationService = new AuthenticationService(userRepository);
        activeUser = new User(
                UserId.of("550e8400-e29b-41d4-a716-446655440000"),
                new EmailAddress("admin@k12.com"),
                new PasswordHash("$2a$12$BEtjQc8Tux4yCcAUflElgOWwgtWjeQFThnfijfp46HbZYc6N3MAoe"),
                Set.of(UserRole.SUPER_ADMIN),
                UserStatus.ACTIVE,
                new UserName("Admin User"));
    }

    @Test
    void shouldAuthenticateUserWithValidCredentials() {
        when(userRepository.findByEmailAddress("admin@k12.com")).thenReturn(Optional.of(activeUser));
        Result<User, AuthenticationError> result = authenticationService.authenticate("admin@k12.com", "password");
        assertTrue(result.isSuccess());
        assertEquals(activeUser, result.get());
    }

    @Test
    void shouldFailWithInvalidCredentials() {
        when(userRepository.findByEmailAddress("admin@k12.com")).thenReturn(Optional.of(activeUser));
        Result<User, AuthenticationError> result = authenticationService.authenticate("admin@k12.com", "wrongpassword");
        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.InvalidCredentials);
    }

    @Test
    void shouldFailWhenUserNotFound() {
        when(userRepository.findByEmailAddress("nonexistent@example.com")).thenReturn(Optional.empty());
        Result<User, AuthenticationError> result =
                authenticationService.authenticate("nonexistent@example.com", "password");
        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.UserNotFound);
    }

    @Test
    void shouldFailWhenUserSuspended() {
        User suspendedUser = new User(
                activeUser.userId(),
                activeUser.emailAddress(),
                activeUser.passwordHash(),
                activeUser.userRole(),
                UserStatus.SUSPENDED,
                activeUser.name());
        when(userRepository.findByEmailAddress("admin@k12.com")).thenReturn(Optional.of(suspendedUser));
        Result<User, AuthenticationError> result = authenticationService.authenticate("admin@k12.com", "password");
        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.UserSuspended);
    }
}
