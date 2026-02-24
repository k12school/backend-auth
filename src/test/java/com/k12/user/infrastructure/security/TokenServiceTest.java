package com.k12.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.*;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TokenServiceTest {
    @Test
    void shouldGenerateTokenWithRequiredClaims() {
        User user = createTestUser();
        String token = TokenService.generateToken(user, "test-tenant");
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void shouldIncludeUserIdInToken() {
        User user = createTestUser();
        String token = TokenService.generateToken(user, "test-tenant");
        String userId = TokenService.extractClaim(token, "sub");
        assertEquals(user.userId().value().toString(), userId);
    }

    @Test
    void shouldIncludeEmailInToken() {
        User user = createTestUser();
        String token = TokenService.generateToken(user, "test-tenant");
        String email = TokenService.extractClaim(token, "email");
        assertEquals(user.emailAddress().value(), email);
    }

    @Test
    void shouldIncludeRolesAsArrayInToken() {
        User user = createTestUser();
        String token = TokenService.generateToken(user, "test-tenant");

        // Decode JWT payload
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have 3 parts");

        String payloadJson =
                new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);

        // Verify roles is now a JSON array
        assertTrue(payloadJson.contains("\"roles\":["), "roles should be a JSON array");
        assertTrue(payloadJson.contains("\"groups\":["), "groups should be a JSON array");
        assertTrue(payloadJson.contains("SUPER_ADMIN"), "Should contain SUPER_ADMIN role");
    }

    private User createTestUser() {
        return new User(
                new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new EmailAddress("test@example.com"),
                new PasswordHash("$2a$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6"),
                Set.of(UserRole.SUPER_ADMIN),
                UserStatus.ACTIVE,
                new UserName("Test User"));
    }
}
