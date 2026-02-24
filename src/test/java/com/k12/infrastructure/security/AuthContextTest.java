package com.k12.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.TenantId;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthContextTest {

    @Test
    void shouldCreateAuthContextWithValidData() {
        TenantId tenantId = TenantId.of("test-tenant-123");
        Set<String> roles = Set.of("ADMIN", "TEACHER");

        AuthContext context = new AuthContext(tenantId, roles);

        assertTrue(context.getTenantId().isPresent());
        assertEquals(tenantId, context.getTenantId().get());
        assertEquals(2, context.getRoles().size());
        assertTrue(context.getRoles().contains("ADMIN"));
        assertTrue(context.getRoles().contains("TEACHER"));
    }

    @Test
    void shouldHandleNullTenantId() {
        AuthContext context = new AuthContext(null, Set.of("ADMIN"));

        assertTrue(context.getTenantId().isEmpty());
        assertEquals(1, context.getRoles().size());
    }

    @Test
    void shouldHandleNullRoles() {
        TenantId tenantId = TenantId.of("test-tenant-123");

        AuthContext context = new AuthContext(tenantId, null);

        assertTrue(context.getTenantId().isPresent());
        assertTrue(context.getRoles().isEmpty());
    }

    @Test
    void shouldReturnCorrectHasRole() {
        AuthContext context = new AuthContext(TenantId.of("tenant-123"), Set.of("ADMIN", "TEACHER"));

        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasRole("TEACHER"));
        assertFalse(context.hasRole("STUDENT"));
    }

    @Test
    void shouldReturnCorrectIsTenant() {
        TenantId tenantId = TenantId.of("tenant-123");
        AuthContext context = new AuthContext(tenantId, Set.of());

        assertTrue(context.isTenant(TenantId.of("tenant-123")));
        assertFalse(context.isTenant(TenantId.of("other-tenant")));
    }

    @Test
    void shouldReturnEmptyOptionalForNullTenantWhenCheckingIsTenant() {
        AuthContext context = new AuthContext(null, Set.of());

        assertFalse(context.isTenant(TenantId.of("any-tenant")));
    }

    @Test
    void shouldReturnImmutableRolesSet() {
        Set<String> mutableRoles = new HashSet<>(Set.of("ADMIN"));
        AuthContext context = new AuthContext(TenantId.of("tenant"), mutableRoles);

        mutableRoles.add("TEACHER");

        assertFalse(context.getRoles().contains("TEACHER"));
        assertThrows(
                UnsupportedOperationException.class, () -> context.getRoles().add("STUDENT"));
    }
}
