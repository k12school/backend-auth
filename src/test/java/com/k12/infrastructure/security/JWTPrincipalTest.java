package com.k12.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.TenantId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JWTPrincipalTest {

    @Test
    void shouldCreateJWTPrincipalWithTenantId() {
        TenantId tenantId = TenantId.of("tenant-123");
        JWTPrincipal principal = new JWTPrincipal("user-123", "user@example.com", Set.of("ADMIN"), tenantId);

        assertEquals("user-123", principal.getUserId());
        assertEquals("user@example.com", principal.getEmail());
        assertEquals("user-123", principal.getName());
        assertEquals(tenantId, principal.getTenantId());
        assertTrue(principal.hasRole("ADMIN"));
    }

    @Test
    void shouldHandleNullTenantId() {
        JWTPrincipal principal = new JWTPrincipal("user-123", "user@example.com", Set.of("ADMIN"), null);

        assertEquals("user-123", principal.getUserId());
        assertNull(principal.getTenantId());
    }

    @Test
    void shouldHandleNullRoles() {
        TenantId tenantId = TenantId.of("tenant-123");
        JWTPrincipal principal = new JWTPrincipal("user-123", "user@example.com", null, tenantId);

        assertTrue(principal.getRoles().isEmpty());
    }

    @Test
    void shouldReturnTenantIdValueObject() {
        TenantId tenantId = TenantId.of("tenant-456");
        JWTPrincipal principal = new JWTPrincipal("user-123", "user@example.com", Set.of(), tenantId);

        assertNotNull(principal.getTenantId());
        assertTrue(principal.getTenantId() instanceof TenantId);
        assertEquals("tenant-456", principal.getTenantId().value());
    }
}
