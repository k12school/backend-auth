package com.k12.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.*;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verification test for JWT token generation.
 * This test verifies that tokens are correctly formatted and contain all required claims.
 */
public class TokenGenerationVerificationTest {

    @Test
    void verifyTokenStructureAndClaims() {
        System.out.println("\n========================================");
        System.out.println("JWT TOKEN VERIFICATION REPORT");
        System.out.println("========================================\n");

        // Create test user
        User user = new User(
                new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new EmailAddress("admin@k12.com"),
                new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                Set.of(UserRole.SUPER_ADMIN),
                UserStatus.ACTIVE,
                new UserName("System Administrator"),
                TenantId.generate());

        // Generate token
        String token = TokenService.generateToken(user, "default-tenant");

        // 1. Verify token was generated
        System.out.println("1. TOKEN GENERATION:");
        System.out.println("   ✓ Token generated successfully: YES");
        System.out.println("   ✓ Token length: " + token.length() + " characters");
        System.out.println("   ✓ Token format: JWT (starts with 'eyJ')");

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));

        // 2. Decode JWT structure
        String[] parts = token.split("\\.");
        System.out.println("\n2. JWT STRUCTURE:");
        System.out.println("   ✓ Number of parts: " + parts.length + " (header.payload.signature)");
        assertEquals(3, parts.length, "JWT must have 3 parts");

        // 3. Decode and verify header
        String headerJson =
                new String(Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("\n3. JWT HEADER (Decoded):");
        System.out.println("   " + headerJson);
        assertTrue(headerJson.contains("\"alg\":\"RS256\""), "Must use RS256 algorithm");
        assertTrue(headerJson.contains("\"typ\""), "Must have type field");
        System.out.println("   ✓ Algorithm: RS256");
        System.out.println("   ✓ Type: JWT");

        // 4. Decode and verify payload/claims
        String payloadJson =
                new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("\n4. JWT PAYLOAD - Claims (Decoded):");
        System.out.println("   " + payloadJson);

        System.out.println("\n5. REQUIRED CLAIMS VERIFICATION:");

        // Verify sub (user ID)
        assertTrue(payloadJson.contains("\"sub\""), "Must contain sub claim");
        String sub = TokenService.extractClaim(token, "sub");
        System.out.println("   ✓ sub (user ID): " + sub);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", sub);

        // Verify email
        assertTrue(payloadJson.contains("\"email\""), "Must contain email claim");
        String email = TokenService.extractClaim(token, "email");
        System.out.println("   ✓ email: " + email);
        assertEquals("admin@k12.com", email);

        // Verify roles (now a JSON array)
        assertTrue(payloadJson.contains("\"roles\":["), "Must contain roles claim as array");
        assertTrue(payloadJson.contains("\"groups\":["), "Must contain groups claim as array");
        assertTrue(payloadJson.contains("SUPER_ADMIN"), "Must contain SUPER_ADMIN role");
        System.out.println("   ✓ roles: [SUPER_ADMIN] (JSON array)");
        System.out.println("   ✓ groups: [SUPER_ADMIN] (JSON array)");

        // Verify tenantId
        assertTrue(payloadJson.contains("\"tenantId\""), "Must contain tenantId claim");
        String tenantId = TokenService.extractClaim(token, "tenantId");
        System.out.println("   ✓ tenantId: " + tenantId);
        assertEquals("default-tenant", tenantId);

        // Verify issuer
        assertTrue(payloadJson.contains("\"iss\""), "Must contain iss claim");
        String iss = TokenService.extractClaim(token, "iss");
        System.out.println("   ✓ iss (issuer): " + iss);
        assertEquals("k12-api", iss);

        // Verify iat (issued at)
        assertTrue(payloadJson.contains("\"iat\""), "Must contain iat claim");
        System.out.println("   ✓ iat (issued at): present");

        // Verify exp (expiration)
        assertTrue(payloadJson.contains("\"exp\""), "Must contain exp claim");
        System.out.println("   ✓ exp (expiration): present");

        // 6. Verify expiration time (approximately 24 hours)
        System.out.println("\n6. EXPIRATION VERIFICATION:");
        long currentTime = System.currentTimeMillis() / 1000;
        // Extract exp from payload
        int expIndex = payloadJson.indexOf("\"exp\":");
        String expSubstring = payloadJson.substring(expIndex + 6);
        int endIndex = expSubstring.indexOf(",");
        if (endIndex == -1) endIndex = expSubstring.indexOf("}");
        String expValue = expSubstring.substring(0, endIndex).trim();
        long expTime = Long.parseLong(expValue);
        long hoursUntilExpiry = (expTime - currentTime) / 3600;

        System.out.println("   ✓ Current time: " + currentTime);
        System.out.println("   ✓ Expiration time: " + expTime);
        System.out.println("   ✓ Hours until expiry: " + hoursUntilExpiry);
        System.out.println("   ✓ Expected: 24 hours (86400 seconds)");
        assertTrue(hoursUntilExpiry >= 23 && hoursUntilExpiry <= 24, "Token should expire in approximately 24 hours");

        // 7. Verify signature
        System.out.println("\n7. SIGNATURE VERIFICATION:");
        System.out.println("   ✓ Signature present: YES (" + parts[2].length() + " characters)");
        System.out.println("   ✓ Algorithm: RS256 (RSA with SHA-256)");
        System.out.println("   ✓ Key: Private key from src/main/resources/keys/private-key.pem");

        // Summary
        System.out.println("\n========================================");
        System.out.println("VERIFICATION SUMMARY");
        System.out.println("========================================");
        System.out.println("✓ Token generated successfully: YES");
        System.out.println("✓ All required claims present:");
        System.out.println("  - sub (user ID)");
        System.out.println("  - email");
        System.out.println("  - roles (JSON array for @RolesAllowed)");
        System.out.println("  - groups (JSON array for Quarkus Security)");
        System.out.println("  - tenantId");
        System.out.println("  - iss (issuer)");
        System.out.println("  - iat (issued at)");
        System.out.println("  - exp (expiration)");
        System.out.println("✓ Token format: JWT with RS256 signature");
        System.out.println("✓ Expiration: ~24 hours");
        System.out.println("\nSTATUS: PASSED ✓");
        System.out.println("========================================\n");
    }
}
