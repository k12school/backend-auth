package com.k12.common.infrastructure.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Security Context Filter that extracts JWT information and populates ThreadLocal.
 *
 * <p>This filter:
 * <ul>
 *   <li>Extracts user info from Quarkus JsonWebToken</li>
 *   <li>Populates ThreadLocal for static access</li>
 *   <li>Cleans up ThreadLocal after request completes</li>
 * </ul>
 */
@Slf4j
@Provider
@Priority(Priorities.AUTHENTICATION) // Run before @RolesAllowed
@Dependent
public class SecurityContextFilter implements ContainerRequestFilter {

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        log.debug("=== SecurityContextFilter ===");
        log.debug("Request: {} {}", method, path);
        log.debug("JWT object present: {}", jwt != null);

        // Skip security context setup for authentication endpoints
        // (e.g., login where no JWT exists yet)
        if (isAuthenticationPath(requestContext)) {
            log.debug("Skipping security context for authentication path");
            return;
        }

        if (jwt != null) {
            try {
                // Extract user ID from JWT subject claim
                String userId = jwt.getSubject();
                if (userId != null && !userId.isBlank()) {
                    SecurityContext.setCurrentUserId(userId);
                }

                // Extract roles from JWT claim
                Set<String> roles = extractRoles();
                if (!roles.isEmpty()) {
                    SecurityContext.setCurrentRoles(roles);
                }

                // Extract tenantId from JWT claim
                String tenantId = jwt.getClaim("tenantId");
                if (tenantId != null && !tenantId.isBlank()) {
                    SecurityContext.setCurrentTenantId(com.k12.common.domain.model.TenantId.of(tenantId));
                }

                log.debug("Security context populated for user: {} with roles: {}", userId, roles);
            } catch (Exception e) {
                log.warn("Failed to populate security context: {}", e.getMessage());
            }
        }
    }

    /**
     * Checks if the request path is for authentication (should skip security setup).
     */
    private boolean isAuthenticationPath(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        return path.equals("/api/auth/login") || path.equals("/api/auth/register");
    }

    /**
     * Extracts roles from JWT claim.
     * Uses "groups" claim (Quarkus/Jakarta Security standard) with fallback to "roles".
     *
     * @return Set of role names
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractRoles() {
        try {
            // Try "groups" first (Quarkus @RolesAllowed standard)
            Object groupsClaim = jwt.getClaim("groups");
            if (groupsClaim != null) {
                return extractRolesFromClaim(groupsClaim);
            }

            // Fallback to "roles" claim
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim != null) {
                return extractRolesFromClaim(rolesClaim);
            }

            return Set.of();
        } catch (Exception e) {
            log.warn("Failed to extract roles from JWT: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Extract roles from a claim object (handles Set, List, String).
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractRolesFromClaim(Object claim) {
        // Handle JSON array (returned as Set by Quarkus)
        if (claim instanceof Set) {
            return (Set<String>) claim;
        }

        // Handle List type
        if (claim instanceof java.util.Collection) {
            return new HashSet<>((java.util.Collection<String>) claim);
        }

        // Handle String format (comma-separated or space-separated)
        if (claim instanceof String) {
            String rolesString = (String) claim;
            if (rolesString.isEmpty()) {
                return Set.of();
            }
            Set<String> roles = new HashSet<>();
            for (String role : rolesString.split("[,\\s]+")) {
                if (!role.isBlank()) {
                    roles.add(role.trim());
                }
            }
            return roles;
        }

        log.debug("Unhandled roles claim type: {}", claim.getClass());
        return Set.of();
    }
}
