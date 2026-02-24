package com.k12.infrastructure.security;

import com.k12.common.domain.model.TenantId;
import java.util.Optional;
import java.util.Set;

/**
 * Request-scoped authentication context providing access to tenant and role information.
 *
 * <p>This bean is automatically populated by {@link JWTAuthenticationFilter}
 * and can be injected anywhere via CDI:
 *
 * <pre>{@code
 * @Inject
 * private AuthContext authContext;
 *
 * public void someMethod() {
 *     Optional<TenantId> tenantId = authContext.getTenantId();
 *     Set<String> roles = authContext.getRoles();
 * }
 * }</pre>
 *
 * <p>The context is created per-request and destroyed at the end of the request,
 * ensuring thread-safe isolation between concurrent requests.
 *
 * <p>Note: This class is produced via {@link AuthContextProducer} as a request-scoped bean.
 */
public class AuthContext {

    private final Optional<TenantId> tenantId;
    private final Set<String> roles;

    /**
     * Creates a new AuthContext with tenant and role information.
     *
     * @param tenantId The tenant ID (can be null for users without tenant context)
     * @param roles The set of roles (can be null, will be treated as empty set)
     */
    public AuthContext(TenantId tenantId, Set<String> roles) {
        this.tenantId = Optional.ofNullable(tenantId);
        this.roles = roles != null ? Set.copyOf(roles) : Set.of();
    }

    /**
     * Gets the tenant ID for the current request.
     *
     * @return Optional containing TenantId, or empty if not present
     */
    public Optional<TenantId> getTenantId() {
        return tenantId;
    }

    /**
     * Gets the roles for the current request.
     *
     * @return Set of role names (never null, may be empty)
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role The role name to check
     * @return true if the user has the role
     */
    public boolean hasRole(String role) {
        return role != null && roles.contains(role);
    }

    /**
     * Checks if the current context belongs to a specific tenant.
     *
     * @param id The tenant ID to compare against
     * @return true if this context belongs to the specified tenant
     */
    public boolean isTenant(TenantId id) {
        return id != null && tenantId.isPresent() && tenantId.get().equals(id);
    }
}
