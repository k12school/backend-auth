package com.k12.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.k12.common.domain.model.TenantId;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Set;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JWT authentication filter.
 */
class JWTAuthenticationFilterUnitTest {

    private final JWTAuthenticationFilter filter = new JWTAuthenticationFilter();

    @Test
    void shouldExtractTenantIdAsValueObject() throws Exception {
        String token = createMockJWT("user-123", "user@example.com", "ADMIN", "tenant-456");

        ContainerRequestContext requestContext = mockRequestContext(token);
        filter.filter(requestContext);

        SecurityContext securityContext = requestContext.getSecurityContext();
        assertNotNull(securityContext);
        JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
        assertNotNull(principal);
        assertNotNull(principal.tenantId());
        assertTrue(principal.tenantId() instanceof TenantId);
        assertEquals("tenant-456", principal.tenantId().value());
    }

    @Test
    void shouldSetAuthContextProperty() throws Exception {
        String token = createMockJWT("user-123", "user@example.com", "ADMIN", "tenant-456");

        ContainerRequestContext requestContext = mockRequestContext(token);
        filter.filter(requestContext);

        AuthContext authContext = (AuthContext) requestContext.getProperty("auth.context");
        assertNotNull(authContext);
        assertTrue(authContext.tenantId().isPresent());
        assertEquals("tenant-456", authContext.tenantId().get().value());
        assertTrue(authContext.hasRole("ADMIN"));
    }

    @Test
    void shouldSetTenantIdAndRolesAsProperties() throws Exception {
        String token = createMockJWT("user-123", "user@example.com", "ADMIN,TEACHER", "tenant-789");

        ContainerRequestContext requestContext = mockRequestContext(token);
        filter.filter(requestContext);

        TenantId tenantId = (TenantId) requestContext.getProperty("tenantId");
        assertNotNull(tenantId);
        assertEquals("tenant-789", tenantId.value());

        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) requestContext.getProperty("roles");
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("TEACHER"));
    }

    @Test
    void shouldHandleMissingTenantIdGracefully() throws Exception {
        String token = createMockJWT("user-123", "user@example.com", "ADMIN", null);

        ContainerRequestContext requestContext = mockRequestContext(token);
        filter.filter(requestContext);

        SecurityContext securityContext = requestContext.getSecurityContext();
        JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

        assertNull(principal.tenantId());

        AuthContext authContext = (AuthContext) requestContext.getProperty("auth.context");
        assertTrue(authContext.tenantId().isEmpty());
    }

    // Helper methods for unit tests

    private String createMockJWT(String userId, String email, String roles, String tenantId) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(userId);
        claims.setStringClaim("email", email);
        claims.setStringClaim("roles", roles);
        if (tenantId != null) {
            claims.setStringClaim("tenantId", tenantId);
        }

        // Set expiration time 1 hour in the future
        claims.setExpirationTimeMinutesInTheFuture(60);

        // Generate a mock JWT (for testing purposes, we use a simple HMAC signature)
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(new HmacKey("test-secret-key-for-testing-only-32bytes".getBytes()));
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

        return jws.getCompactSerialization();
    }

    private ContainerRequestContext mockRequestContext(String token) throws Exception {
        ContainerRequestContext mockContext = mock(ContainerRequestContext.class);

        // Mock headers
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Authorization", "Bearer " + token);
        when(mockContext.getHeaders()).thenReturn(headers);

        // Mock URI info
        UriInfo mockUriInfo = mock(UriInfo.class);
        when(mockUriInfo.getRequestUri()).thenReturn(new URI("http://localhost:8080/api/test"));
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);

        // Capture security context when set
        doAnswer(invocation -> {
                    SecurityContext ctx = invocation.getArgument(0);
                    when(mockContext.getSecurityContext()).thenReturn(ctx);
                    return null;
                })
                .when(mockContext)
                .setSecurityContext(any());

        // Capture properties when set
        doAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Object value = invocation.getArgument(1);
                    when(mockContext.getProperty(key)).thenReturn(value);
                    return null;
                })
                .when(mockContext)
                .setProperty(any(), any());

        return mockContext;
    }
}
