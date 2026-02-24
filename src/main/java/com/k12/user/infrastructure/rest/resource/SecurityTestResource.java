package com.k12.user.infrastructure.rest.resource;

import com.k12.common.domain.model.TenantId;
import com.k12.infrastructure.security.JWTPrincipal;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Test resource to demonstrate JWT filter functionality.
 */
@Path("/api/security")
public class SecurityTestResource {

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
}
