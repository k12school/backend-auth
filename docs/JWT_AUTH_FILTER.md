# JWT Authentication Filter Implementation

## Overview

A JWT authentication filter has been implemented that captures JWT tokens from HTTP requests, validates them, and makes the JWT claims available throughout the application via the JAX-RS SecurityContext.

## Components

### 1. JWTAuthenticationFilter
**Location:** `com.k12.infrastructure.security.JWTAuthenticationFilter`

A JAX-RS ContainerRequestFilter that:
- Extracts JWT from `Authorization: Bearer <token>` header
- Validates token signature and expiration using jose4j
- Extracts claims: userId (sub), email, roles, tenantId
- Creates a custom SecurityContext with JWT claims
- Skips authentication if no token is provided (doesn't enforce auth)

### 2. JWTSecurityContext
**Location:** `com.k12.infrastructure.security.JWTSecurityContext`

Custom SecurityContext implementation that:
- Wraps a JWTPrincipal with user information
- Provides role-based authorization via `isUserInRole()`
- Returns JWT authentication scheme

### 3. JWTPrincipal
**Location:** `com.k12.infrastructure.security.JWTPrincipal`

Principal implementation containing:
- `getUserId()` - User ID from JWT subject
- `getEmail()` - User email from JWT claim
- `getRoles()` - Set of user roles
- `getTenantId()` - **TenantId value object** for multi-tenancy (changed from String)
- `hasRole(String)` - Check if user has specific role

### 4. SecurityTestResource
**Location:** `com.k12.user.infrastructure.rest.resource.SecurityTestResource`

Example resource demonstrating JWT filter usage.

## Usage

### In REST Endpoints

```java
@Path("/api/myresource")
public class MyResource {

    @GET
    @Path("/protected")
    public Response getProtected(@Context SecurityContext securityContext) {
        // Check if user is authenticated
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(401).build();
        }

        // Cast to JWTPrincipal
        JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

        // Access JWT claims
        String userId = principal.getUserId();
        String email = principal.getEmail();
        Set<String> roles = principal.getRoles();
        TenantId tenantId = principal.getTenantId();
        if (tenantId != null) {
            String id = tenantId.value();
            // Use tenant ID
        }

        // Check roles
        if (principal.hasRole("SUPER_ADMIN")) {
            // Admin logic
        }

        // Or use SecurityContext method
        if (securityContext.isUserInRole("TEACHER")) {
            // Teacher logic
        }

        return Response.ok().build();
    }
}
```

### Role-Based Authorization

```java
@GET
@Path("/admin-only")
public Response adminEndpoint(@Context SecurityContext securityContext) {
    if (!securityContext.isUserInRole("SUPER_ADMIN")) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity("{\"error\":\"FORBIDDEN\"}")
            .build();
    }
    // Admin logic here
}
```

### Optional Authentication

The filter does NOT enforce authentication - it only processes JWT tokens if present. To require authentication:

```java
@GET
@Path("/require-auth")
public Response requireAuth(@Context SecurityContext securityContext) {
    if (securityContext.getUserPrincipal() == null) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity("{\"error\":\"UNAUTHORIZED\"}")
            .build();
    }
    // Protected logic here
}
```

### Access Patterns

The JWT authentication filter provides three patterns for accessing tenant and role information:

#### Pattern 1: CDI Injection (Recommended)

The simplest way to access tenant and role information:

```java
@Path("/api/resource")
public class MyResource {

    @Inject
    private AuthContext authContext;

    @GET
    public Response getData() {
        Optional<TenantId> tenantId = authContext.getTenantId();
        Set<String> roles = authContext.getRoles();

        if (authContext.hasRole("ADMIN")) {
            // Admin logic
        }

        return Response.ok().build();
    }
}
```

#### Pattern 2: SecurityContext (Enhanced)

Access via JAX-RS SecurityContext with type-safe TenantId:

```java
@GET
public Response getData(@Context SecurityContext securityContext) {
    JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

    // Now returns TenantId value object instead of String
    TenantId tenantId = principal.getTenantId();
    if (tenantId != null) {
        String id = tenantId.value();
        // Use tenant ID
    }

    Set<String> roles = principal.getRoles();
    return Response.ok().build();
}
```

#### Pattern 3: Request Properties

Direct access from ContainerRequestContext:

```java
@GET
public Response getData(@Context ContainerRequestContext requestContext) {
    TenantId tenantId = (TenantId) requestContext.getProperty("tenantId");
    Set<String> roles = (Set<String>) requestContext.getProperty("roles");

    return Response.ok().build();
}
```

## Testing

### Test Endpoints

```bash
# 1. Login to get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@k12.com","password":"admin123"}' \
  | jq -r '.token')

# 2. Access protected endpoint with token
curl -X GET http://localhost:8080/api/security/whoami \
  -H "Authorization: Bearer $TOKEN"

# 3. Access without token (returns 401)
curl -X GET http://localhost:8080/api/security/whoami

# 4. Check admin role
curl -X GET http://localhost:8080/api/security/check-admin \
  -H "Authorization: Bearer $TOKEN"
```

## JWT Token Structure

The JWT generated by the login endpoint contains:

```json
{
  "sub": "user-id-uuid",
  "email": "user@example.com",
  "roles": "SUPER_ADMIN,TEACHER",
  "tenantId": "tenant-uuid",
  "iat": 1234567890,
  "exp": 1234654290,
  "iss": "k12-api",
  "jti": "unique-jwt-id"
}
```

## Notes

- The filter uses jose4j for JWT validation
- Signature verification is delegated to Quarkus (mp.jwt.verify.publickey.location)
- The filter runs at PRIORITY.AUTHENTICATION (1000)
- Clock skew tolerance: 30 seconds
- If JWT validation fails, the filter logs a warning and continues without setting the SecurityContext

## Future Enhancements

Possible improvements:
1. Add `@Authenticated` annotation to enforce authentication on endpoints
2. Add method-level authorization annotations like `@RequireRole("SUPER_ADMIN")`
3. Cache public key to avoid re-reading on every request
4. Add JWT refresh token support
5. Implement token revocation/blacklist

## Dependencies

- jose4j - JWT validation
- MicroProfile JWT - Quarkus integration
- jaxrs - JAX-RS API
