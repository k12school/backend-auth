package com.k12.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.k12.common.domain.model.Result;
import com.k12.user.application.dto.LoginRequest;
import com.k12.user.application.dto.LoginResponse;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.User;
import com.k12.user.domain.service.AuthenticationService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationApplicationServiceTest {
    private AuthenticationService authenticationService;
    private AuthenticationApplicationService applicationService;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        applicationService = new AuthenticationApplicationService(authenticationService);
    }

    @Test
    void shouldReturnTokenOnValidLogin() {
        LoginRequest request = new LoginRequest("admin@k12.com", "password123");
        User mockUser = createMockUser();
        when(authenticationService.authenticate("admin@k12.com", "password123")).thenReturn(Result.success(mockUser));

        Result<LoginResponse, AuthenticationError> result = applicationService.login(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.get().token());
        assertEquals("admin@k12.com", result.get().user().email());
    }

    @Test
    void shouldReturnErrorOnInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin@k12.com", "wrongpassword");
        when(authenticationService.authenticate("admin@k12.com", "wrongpassword"))
                .thenReturn(Result.failure(new AuthenticationError.InvalidCredentials()));

        Result<LoginResponse, AuthenticationError> result = applicationService.login(request);

        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.InvalidCredentials);
    }

    private User createMockUser() {
        return new User(
                new com.k12.common.domain.model.UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new com.k12.user.domain.models.EmailAddress("admin@k12.com"),
                new com.k12.user.domain.models.PasswordHash(
                        "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5NU9bK8fk.5qS"),
                Set.of(com.k12.user.domain.models.UserRole.SUPER_ADMIN),
                com.k12.user.domain.models.UserStatus.ACTIVE,
                new com.k12.user.domain.models.UserName("Admin"));
    }
}
