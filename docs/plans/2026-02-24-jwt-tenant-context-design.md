# JWT Tenant Context Enhancement Design

**Date:** 2026-02-24
**Author:** Design Document
**Status:** Approved

## Overview

Enhance the existing JWT authentication infrastructure to provide type-safe, multi-pattern access to `tenantId` and `roles` throughout the application. This design improves the current JWT filter by adding a CDI-injectable context, using value objects for type safety, and providing multiple access patterns for application code.

## Problem Statement

The current `JWTAuthenticationFilter` extracts `tenantId` and `roles` from JWT tokens and stores them in `JWTPrincipal` as simple types (String and Set). While functional, this approach:

- Lacks type safety for tenant ID (String instead of value object)
- Requires manual extraction from SecurityContext in every endpoint
- Doesn't provide CDI injection support for tenant context
- Mixes data access patterns (SecurityContext vs direct properties)

## Goals

1. **Type Safety:** Use `TenantId` value object instead of raw String
2. **Multiple Access Patterns:** Support CDI injection, SecurityContext, and request properties
3. **Convenience:** Provide easy-to-use context bean for tenant and role information
4. **Backward Compatibility:** Maintain existing SecurityContext-based access
5. **Thread Safety:** Ensure stateless, request-scoped behavior

## Architecture

### Component Diagram

```
HTTP Request (JWT with tenantId, roles)
    │
    ▼
JWTAuthenticationFilter
    │
    ├─► Extracts claims from JWT
    │   - tenantId: "uuid-string"
    │   - roles: "ADMIN,TEACHER"
    │
    ├─► Creates TenantId value object
    │   TenantId.of("uuid-string")
    │
    ├─► Instantiates AuthContext (@RequestScoped)
    │   new AuthContext(tenantId, roles)
    │
    ├─► Creates JWTPrincipal (enhanced)
    │   new JWTPrincipal(userId, email, roles, tenantId)
    │
    └─► Sets ContainerRequestContext properties
        ├─► setSecurityContext(context)
        ├─► setProperty("tenantId", tenantId)
        ├─► setProperty("roles", roles)
        ├─► setProperty("auth.context", authContext)
        └─► setProperty("jwt.claims", claims) [existing]
```

### Request Scope Isolation

Each HTTP request gets isolated instances:
- One `AuthContext` per request (`@RequestScoped`)
- One `JWTPrincipal` per request
- One `ContainerRequestContext` per request
- No shared state between concurrent requests

**Thread Safety:** Fully stateless and thread-safe by design.

## Components

### 1. AuthContext (New)

**Package:** `com.k12.infrastructure.security`
**Scope:** `@RequestScoped`

```java
@RequestScoped
public class AuthContext {

    private final Optional<TenantId> tenantId;
    private final Set<String> roles;

    public AuthContext(TenantId tenantId, Set<String> roles) {
        this.tenantId = Optional.ofNullable(tenantId);
        this.roles = roles != null ? roles : Set.of();
    }

    public Optional<TenantId> getTenantId() {
        return tenantId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isTenant(TenantId id) {
        return tenantId.isPresent() && tenantId.get().equals(id);
    }
}
```

**Responsibilities:**
- Wraps tenant and role information for easy access
- Provides null-safe access via `Optional<TenantId>`
- Offers convenience methods for role/tenant checking
- Available via CDI injection anywhere in the application

### 2. JWTPrincipal (Enhanced)

**Package:** `com.k12.infrastructure.security`

**Changes:**
- Replace `String tenantId` with `TenantId tenantId`
- Update `getTenantId()` to return `TenantId` instead of `String`
- Support null for optional tenant context

```java
public class JWTPrincipal implements Principal {

    private final String userId;
    private final String email;
    private final Set<String> roles;
    private final TenantId tenantId;  // Changed from String

    public JWTPrincipal(String userId, String email, Set<String> roles, TenantId tenantId) {
        this.userId = userId;
        this.email = email;
        this.roles = roles != null ? roles : Set.of();
        this.tenantId = tenantId;  // Can be null
    }

    public TenantId getTenantId() {  // Changed return type
        return tenantId;
    }

    // ... other methods unchanged
}
```

### 3. JWTAuthenticationFilter (Enhanced)

**Package:** `com.k12.infrastructure.security`

**Changes:**
- Convert String tenantId to `TenantId` value object
- Create and store `AuthContext` instance
- Set additional properties on `ContainerRequestContext`

**Key modifications:**
```java
// In filter() method, after parsing JWT claims:

// Convert String to TenantId value object
TenantId tenantId = null;
try {
    String tenantIdString = getStringClaimSafely(claims, "tenantId");
    if (tenantIdString != null) {
        tenantId = TenantId.of(tenantIdString);
    }
} catch (IllegalArgumentException e) {
    LOGGER.warn("Invalid tenantId format in JWT: {}", e.getMessage());
}

Set<String> roles = extractRoles(claims);

// Create AuthContext bean
AuthContext authContext = new AuthContext(tenantId, roles);

// Create security context with TenantId
JWTSecurityContext securityContext = new JWTSecurityContext(
    claims.getSubject(),
    getStringClaimSafely(claims, "email"),
    roles,
    tenantId,  // Pass TenantId instead of String
    requestContext.getUriInfo().getRequestUri()
);

// Set all access patterns
requestContext.setSecurityContext(securityContext);
requestContext.setProperty("tenantId", tenantId);
requestContext.setProperty("roles", roles);
requestContext.setProperty("auth.context", authContext);
requestContext.setProperty("jwt.claims", claims);  // Existing
```

### 4. JWTSecurityContext (No Changes)

**Package:** `com.k12.infrastructure.security`

No changes required - it already wraps `JWTPrincipal` and will automatically use the enhanced version with `TenantId`.

## Data Flow

### Request Processing Sequence

```
1. HTTP Request with Authorization: Bearer <jwt>
   │
2. JWTAuthenticationFilter.filter() invoked (Priority: AUTHENTICATION)
   │
3. Extract Bearer token from Authorization header
   │
4. validateAndParseToken(token)
   │  └─> Returns JwtClaims
   │       {
   │         "sub": "user-uuid",
   │         "email": "user@example.com",
   │         "roles": "SUPER_ADMIN,TEACHER",
   │         "tenantId": "tenant-uuid",
   │         ...
   │       }
   │
5. Extract and convert data:
   │  - userId = claims.getSubject()
   │  - email = claims.getStringClaimValue("email")
   │  - roles = extractRoles(claims) → Set<String>
   │  - tenantId = TenantId.of(claims.getStringClaimValue("tenantId"))
   │
6. Create AuthContext:
   │  AuthContext authContext = new AuthContext(tenantId, roles)
   │
7. Create JWTPrincipal with TenantId:
   │  JWTPrincipal principal = new JWTPrincipal(userId, email, roles, tenantId)
   │
8. Create JWTSecurityContext:
   │  JWTSecurityContext securityContext = new JWTSecurityContext(...)
   │
9. Set all access patterns on ContainerRequestContext:
   ├─> setSecurityContext(securityContext)
   ├─> setProperty("tenantId", tenantId)        // TenantId object
   ├─> setProperty("roles", roles)               // Set<String>
   ├─> setProperty("auth.context", authContext)  // AuthContext bean
   └─> setProperty("jwt.claims", claims)         // Raw claims
   │
10. Request continues to REST endpoint
```

### Access Patterns for Application Code

**Pattern 1: CDI Injection (Recommended)**
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

**Pattern 2: SecurityContext (Enhanced)**
```java
@GET
public Response getData(@Context SecurityContext securityContext) {
    JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

    TenantId tenantId = principal.getTenantId();  // Returns TenantId, not String
    Set<String> roles = principal.getRoles();

    return Response.ok().build();
}
```

**Pattern 3: Request Properties (New)**
```java
@GET
public Response getData(@Context ContainerRequestContext requestContext) {
    TenantId tenantId = (TenantId) requestContext.getProperty("tenantId");
    Set<String> roles = (Set<String>) requestContext.getProperty("roles");

    return Response.ok().build();
}
```

**Pattern 4: Raw JWT Claims (Existing)**
```java
@GET
public Response getData(@Context ContainerRequestContext requestContext) {
    JwtClaims claims = (JwtClaims) requestContext.getProperty("jwt.claims");
    String tenantId = claims.getStringClaimValue("tenantId");

    return Response.ok().build();
}
```

## Error Handling

### Validation & Edge Cases

| Scenario | Handling |
|----------|----------|
| Missing `tenantId` claim in JWT | `TenantId` is null in `JWTPrincipal`; `AuthContext.getTenantId()` returns `Optional.empty()` |
| Invalid `tenantId` format (not UUID, blank) | Caught in filter, logged as warning, `TenantId` set to null |
| Missing `roles` claim | Empty `Set<String>` returned (never null) |
| Malformed `roles` claim | Existing `extractRoles()` returns empty set |
| No JWT token provided | Filter returns early, no security context set |
| JWT validation fails | Warning logged, request continues without authentication |

### Null-Safety Guarantees

```java
// AuthContext provides safe access
Optional<TenantId> getTenantId();  // Never null, always Optional
Set<String> getRoles();            // Never null, may be empty Set
boolean hasRole(String role);      // Safe even with no roles

// JWTPrincipal
TenantId getTenantId();            // Can be null, check before use
Set<String> getRoles();            // Never null, may be empty Set
```

## Testing Strategy

### Unit Tests

1. **AuthContextTest**
   - Test creation with valid data
   - Test with null tenantId
   - Test `getTenantId()` returns Optional
   - Test `getRoles()` returns Set
   - Test `hasRole()` and `isTenant()` convenience methods

2. **JWTPrincipalTest** (Updated)
   - Test construction with `TenantId` value object
   - Test with null tenantId
   - Verify `getTenantId()` returns `TenantId`
   - Test backward compatibility with roles

3. **JWTAuthenticationFilterTest** (Updated)
   - Verify `TenantId` created from JWT claim
   - Verify `AuthContext` created and stored
   - Verify all three access patterns set correctly
   - Test with missing tenantId claim
   - Test with invalid tenantId format
   - Test concurrent requests (thread safety)

### Integration Tests

4. **SecurityTestResource** (Enhanced)
   - Demonstrate CDI injection of `AuthContext`
   - Demonstrate `TenantId` access via SecurityContext
   - Demonstrate property access from ContainerRequestContext
   - Verify all patterns work in the same request

### Manual Testing

```bash
# Generate JWT with tenantId and roles
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@k12.com","password":"admin123"}' | jq -r '.token')

# Test CDI injection endpoint
curl -X GET http://localhost:8080/api/security/test-context \
  -H "Authorization: Bearer $TOKEN"

# Test without tenantId (craft JWT manually)
# Expected: Optional.empty() returned
```

## Implementation Phases

### Phase 1: Create AuthContext Bean
- [ ] Create `AuthContext.java` in `com.k12.infrastructure.security`
- [ ] Add `@RequestScoped` annotation
- [ ] Implement fields, constructor, and methods
- [ ] Add unit tests in `AuthContextTest.java`

### Phase 2: Enhance JWTPrincipal
- [ ] Change `tenantId` field type from `String` to `TenantId`
- [ ] Update constructor to accept `TenantId`
- [ ] Update `getTenantId()` return type
- [ ] Update unit tests in `JWTPrincipalTest.java`

### Phase 3: Update JWTAuthenticationFilter
- [ ] Add conversion logic: String → TenantId
- [ ] Instantiate `AuthContext` with tenantId and roles
- [ ] Set "tenantId" property on ContainerRequestContext
- [ ] Set "roles" property on ContainerRequestContext
- [ ] Set "auth.context" property on ContainerRequestContext
- [ ] Add error handling for invalid tenantId
- [ ] Update unit tests in `JWTAuthenticationFilterTest.java`

### Phase 4: Update Documentation & Examples
- [ ] Update `JWT_AUTH_FILTER.md` with new access patterns
- [ ] Add examples to `SecurityTestResource`
- [ ] Update integration tests

## Backward Compatibility

### Breaking Changes

**Minor Breaking Change:** `JWTPrincipal.getTenantId()` return type changes from `String` to `TenantId`

**Impact:**
- Any code directly calling `principal.getTenantId()` and expecting a `String` will need to be updated
- Code using `principal.getTenantId().toString()` will work without changes

**Mitigation:**
- Search codebase for direct usage of `getTenantId()`
- Update to use `principal.getTenantId().value()` or `@Inject AuthContext`

### Non-Breaking Changes

- SecurityContext-based access still works
- All existing JWT claims remain available via `jwt.claims` property
- Role extraction unchanged
- Filter behavior unchanged (still optional authentication)

## Migration Guide

### Before (String tenantId)

```java
JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
String tenantId = principal.getTenantId();
if (tenantId != null) {
    // Use tenantId string
}
```

### After (TenantId value object)

**Option 1: CDI Injection (Recommended)**
```java
@Inject
AuthContext authContext;

authContext.getTenantId().ifPresent(tenantId -> {
    // Use TenantId value object
    String id = tenantId.value();
});
```

**Option 2: SecurityContext (Updated)**
```java
JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
TenantId tenantId = principal.getTenantId();
if (tenantId != null) {
    String id = tenantId.value();
}
```

**Option 3: Request Properties**
```java
TenantId tenantId = (TenantId) requestContext.getProperty("tenantId");
if (tenantId != null) {
    String id = tenantId.value();
}
```

## Dependencies

**Existing:**
- `jakarta.ws.rs` - JAX-RS API
- `jakarta.annotation.Priority` - Filter priority
- `org.jose4j` - JWT validation
- `com.k12.common.domain.model.TenantId` - Value object

**New:**
- `jakarta.enterprise.context.RequestScoped` - CDI request scope

## Future Enhancements

Possible future improvements:
1. Add `@TenantRequired` annotation for endpoints that require tenant context
2. Add method-level authorization: `@RequireRole("ADMIN")`
3. Cache public key to avoid re-reading on every request
4. Add tenant-based filtering at repository level
5. Support multiple tenants per user (if needed)

## References

- Existing JWT implementation: `docs/JWT_AUTH_FILTER.md`
- TenantId value object: `src/main/java/com/k12/common/domain/model/TenantId.java`
- Authentication design: `docs/plans/2026-02-23-authentication-design.md`
