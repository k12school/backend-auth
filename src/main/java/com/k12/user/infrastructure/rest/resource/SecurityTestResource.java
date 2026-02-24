package com.k12.user.infrastructure.rest.resource;

import com.k12.common.domain.model.TenantId;
import com.k12.infrastructure.security.AuthContext;
import com.k12.infrastructure.security.JWTPrincipal;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test resource to demonstrate JWT filter functionality.
 */
@Path("/api/security")
public class SecurityTestResource {

    @Inject
    private AuthContext authContext;

    @GET
    @Path("/whoami")
    @Produces("application/json")
    public Response whoAmI(@Context SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"UNAUTHORIZED\",\"message\":\"No authentication found\"}")
                    .build();
        }

        JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

        TenantId tenantId = principal.getTenantId();
        return Response.ok(new UserInfo(
                        principal.getUserId(),
                        principal.getEmail(),
                        principal.getRoles(),
                        tenantId != null ? tenantId.value() : null))
                .build();
    }

    @GET
    @Path("/check-admin")
    @Produces("application/json")
    public Response checkAdmin(@Context SecurityContext securityContext) {
        boolean isAdmin = securityContext.isUserInRole("SUPER_ADMIN");

        return Response.ok(new AdminCheck(
                        securityContext.getUserPrincipal() != null,
                        isAdmin,
                        securityContext.getUserPrincipal() != null
                                ? securityContext.getUserPrincipal().getName()
                                : null))
                .build();
    }

    record UserInfo(String userId, String email, java.util.Set<String> roles, String tenantId) {}

    record AdminCheck(boolean authenticated, boolean isAdmin, String userId) {}

    @GET
    @Path("/test-auth-context")
    @Produces("application/json")
    public Response testAuthContext() {
        Optional<TenantId> tenantId = authContext.getTenantId();
        Set<String> roles = authContext.getRoles();

        Map<String, Object> response = new HashMap<>();
        response.put("cdi_tenantId", tenantId.map(TenantId::value).orElse(null));
        response.put("cdi_roles", roles);
        response.put("cdi_has_admin", authContext.hasRole("ADMIN"));

        return Response.ok(response).build();
    }

    @GET
    @Path("/test-all-patterns")
    @Produces("application/json")
    public Response testAllAccessPatterns(
            @Context SecurityContext securityContext, @Context ContainerRequestContext requestContext) {
        Map<String, Object> response = new HashMap<>();

        // Pattern 1: CDI injection
        response.put(
                "cdi_tenantId", authContext.getTenantId().map(TenantId::value).orElse(null));
        response.put("cdi_roles", authContext.getRoles());

        // Pattern 2: SecurityContext
        JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
        if (principal != null) {
            response.put(
                    "sc_tenantId",
                    principal.getTenantId() != null ? principal.getTenantId().value() : null);
            response.put("sc_roles", principal.getRoles());
        }

        // Pattern 3: Request properties
        TenantId propTenantId = (TenantId) requestContext.getProperty("tenantId");
        @SuppressWarnings("unchecked")
        Set<String> propRoles = (Set<String>) requestContext.getProperty("roles");
        response.put("prop_tenantId", propTenantId != null ? propTenantId.value() : null);
        response.put("prop_roles", propRoles);

        return Response.ok(response).build();
    }
}
