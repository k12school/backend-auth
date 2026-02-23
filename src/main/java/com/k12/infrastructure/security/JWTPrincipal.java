package com.k12.infrastructure.security;

import java.security.Principal;
import java.util.Set;

/**
 * Principal implementation that holds JWT claims for authenticated users.
 *
 * <p>This class provides access to:
 * <ul>
 *   <li>User ID (subject from JWT)</li>
 *   <li>User email</li>
 *   <li>User roles</li>
 *   <li>Tenant ID (for multi-tenancy)</li>
 * </ul>
 *
 * <p>Usage in REST endpoints:
 * <pre>{@code
 * @GET
 * @Path("/protected")
 * public Response getProtectedResource(@Context SecurityContext securityContext) {
 *     JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
 *     String userId = principal.getUserId();
 *     String email = principal.getEmail();
 *     Set<String> roles = principal.getRoles();
 *     // ...
 * }
 * }</pre>
 */
public class JWTPrincipal implements Principal {

    private final String userId;
    private final String email;
    private final Set<String> roles;
    private final String tenantId;

    public JWTPrincipal(String userId, String email, Set<String> roles, String tenantId) {
        this.userId = userId;
        this.email = email;
        this.roles = roles != null ? roles : Set.of();
        this.tenantId = tenantId;
    }

    @Override
    public String getName() {
        return userId;
    }

    /**
     * Get the user ID from JWT subject claim.
     *
     * @return User ID (UUID string)
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the user email from JWT email claim.
     *
     * @return User email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get the user roles from JWT roles claim.
     *
     * @return Set of role names (e.g., SUPER_ADMIN, TEACHER)
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Get the tenant ID from JWT tenantId claim.
     *
     * @return Tenant ID or null if not present
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Check if user has a specific role.
     *
     * @param role Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public String toString() {
        return "JWTPrincipal{" + "userId='"
                + userId + '\'' + ", email='"
                + email + '\'' + ", roles="
                + roles + ", tenantId='"
                + tenantId + '\'' + '}';
    }
}
