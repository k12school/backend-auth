package com.k12.common.infrastructure.security;

import com.k12.common.domain.model.TenantId;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for accessing security context information using ThreadLocal.
 *
 * <p>SecurityContextFilter populates ThreadLocal values from Quarkus JsonWebToken.
 * This provides static access to current user info throughout the application.
 *
 * <p>This provides:
 * <ul>
 *   <li>Current user ID from JWT subject claim</li>
 *   <li>User roles from JWT claim</li>
 *   <li>Tenant ID from JWT claim</li>
 *   <li>Role-based authorization checks</li>
 * </ul>
 */
@Slf4j
public class SecurityContext {

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> CURRENT_ROLES = new ThreadLocal<>();
    private static final ThreadLocal<TenantId> CURRENT_TENANT_ID = new ThreadLocal<>();

    /**
     * Get the current user ID from JWT subject claim.
     *
     * @return current user ID
     * @throws IllegalStateException if no user is authenticated
     */
    public static String getCurrentUserId() {
        // First try ThreadLocal (set by filter)
        String userId = CURRENT_USER_ID.get();
        if (userId != null) {
            return userId;
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    /**
     * Get the current user ID from JWT subject claim.
     *
     * @return Optional containing user ID, or empty if not authenticated
     */
    public static Optional<String> getCurrentUserIdOpt() {
        return Optional.ofNullable(CURRENT_USER_ID.get());
    }

    /**
     * Get the current user's roles.
     *
     * @return Set of role names
     * @throws IllegalStateException if no user is authenticated
     */
    public static Set<String> getCurrentRoles() {
        Set<String> roles = CURRENT_ROLES.get();
        if (roles != null) {
            return roles;
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    /**
     * Get the current user's roles.
     *
     * @return Optional containing Set of roles, or empty if not authenticated
     */
    public static Optional<Set<String>> getCurrentRolesOpt() {
        return Optional.ofNullable(CURRENT_ROLES.get());
    }

    /**
     * Get the current tenant ID.
     *
     * @return Optional containing tenant ID, or empty if not set
     */
    public static Optional<TenantId> getCurrentTenantId() {
        return Optional.ofNullable(CURRENT_TENANT_ID.get());
    }

    /**
     * Check if current user has SUPER_ADMIN role.
     *
     * @return true if user is SUPER_ADMIN
     */
    public static boolean isSuperAdmin() {
        Set<String> roles = CURRENT_ROLES.get();
        return roles != null && roles.contains("SUPER_ADMIN");
    }

    /**
     * Check if current user has a specific role.
     *
     * @param role Role name to check
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        Set<String> roles = CURRENT_ROLES.get();
        return roles != null && roles.contains(role);
    }

    /**
     * Set the current user ID for this thread.
     *
     * @param userId the user ID
     */
    public static void setCurrentUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        CURRENT_USER_ID.set(userId);
        log.debug("Set current user ID (ThreadLocal): {}", userId);
    }

    /**
     * Set the current user's roles for this thread.
     *
     * @param roles the set of roles
     */
    public static void setCurrentRoles(Set<String> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }
        CURRENT_ROLES.set(roles);
        log.debug("Set current roles (ThreadLocal): {}", roles);
    }

    /**
     * Set the current tenant ID for this thread.
     *
     * @param tenantId the tenant ID
     */
    public static void setCurrentTenantId(TenantId tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
        log.debug("Set current tenant ID (ThreadLocal): {}", tenantId);
    }

    /**
     * Clear the security context for this thread.
     * Should be called at the end of each request to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_ROLES.remove();
        CURRENT_TENANT_ID.remove();
        log.debug("Cleared ThreadLocal security context");
    }
}
