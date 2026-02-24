package com.k12.infrastructure.security;

import com.k12.common.domain.model.TenantId;
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
public record JWTPrincipal(String userId, String email, Set<String> roles, TenantId tenantId) implements Principal {

    @Override
    public String getName() {
        return userId;
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
                + roles + ", tenantId="
                + tenantId + '}';
    }
}
