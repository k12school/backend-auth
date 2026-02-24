package com.k12.infrastructure.security;

import com.k12.common.domain.model.TenantId;
import java.net.URI;
import java.security.Principal;
import java.util.Set;

/**
 * Custom SecurityContext implementation that holds JWT authentication information.
 *
 * <p>This class provides:
 * <ul>
 *   <li>JWT-based principal with user details</li>
 *   <li>Role-based authorization check</li>
 *   <li>Access to JWT claims via {@link JWTPrincipal}</li>
 * </ul>
 */
public class JWTSecurityContext implements jakarta.ws.rs.core.SecurityContext {

    private final JWTPrincipal principal;
    private final URI requestUri;

    public JWTSecurityContext(String userId, String email, Set<String> roles, TenantId tenantId, URI requestUri) {
        this.principal = new JWTPrincipal(userId, email, roles, tenantId);
        this.requestUri = requestUri;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return requestUri != null && "https".equalsIgnoreCase(requestUri.getScheme());
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }

    /**
     * Get the JWT principal with access to all JWT claims.
     *
     * @return JWTPrincipal containing user information
     */
    public JWTPrincipal getJWTPrincipal() {
        return principal;
    }
}
