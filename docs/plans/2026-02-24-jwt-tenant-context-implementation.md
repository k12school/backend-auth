# JWT Tenant Context Enhancement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enhance JWT authentication to provide type-safe, multi-pattern access to tenantId and roles through a CDI-injectable AuthContext bean, TenantId value object in JWTPrincipal, and request properties.

**Architecture:** Convert JWT tenantId String to TenantId value object, create @RequestScoped AuthContext CDI bean for tenant/role access, and update JWTAuthenticationFilter to set multiple access patterns (CDI, SecurityContext, ContainerRequestContext properties) while maintaining thread-safe, stateless request-scoped behavior.

**Tech Stack:** Jakarta JAX-RS, CDI (Contexts and Dependency Injection), jose4j for JWT validation, Quarkus framework, JUnit 5 for testing.

---

## Prerequisites

Read these docs before starting:
- `docs/plans/2026-02-24-jwt-tenant-context-design.md` - Complete design spec
- `docs/JWT_AUTH_FILTER.md` - Current JWT implementation
- `src/main/java/com/k12/common/domain/model/TenantId.java` - Value object to understand

Existing files to understand:
- `src/main/java/com/k12/infrastructure/security/JWTAuthenticationFilter.java`
- `src/main/java/com/k12/infrastructure/security/JWTPrincipal.java`
- `src/main/java/com/k12/infrastructure/security/JWTSecurityContext.java`
- `src/test/java/com/k12/infrastructure/security/JWTAuthenticationFilterTest.java`

---

### Task 1: Create AuthContext Bean

**Files:**
- Create: `src/main/java/com/k12/infrastructure/security/AuthContext.java`
- Test: `src/test/java/com/k12/infrastructure/security/AuthContextTest.java`

**Step 1: Write the failing test**

Create test file `src/test/java/com/k12/infrastructure/security/AuthContextTest.java`:

```java
package com.k12.infrastructure.security;

import com.k12.common.domain.model.TenantId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthContextTest {

    @Test
    void shouldCreateAuthContextWithValidData() {
        TenantId tenantId = TenantId.of("test-tenant-123");
        Set<String> roles = Set.of("ADMIN", "TEACHER");

        AuthContext context = new AuthContext(tenantId, roles);

        assertTrue(context.getTenantId().isPresent());
        assertEquals(tenantId, context.getTenantId().get());
        assertEquals(2, context.getRoles().size());
        assertTrue(context.getRoles().contains("ADMIN"));
        assertTrue(context.getRoles().contains("TEACHER"));
    }

    @Test
    void shouldHandleNullTenantId() {
        AuthContext context = new AuthContext(null, Set.of("ADMIN"));

        assertTrue(context.getTenantId().isEmpty());
        assertEquals(1, context.getRoles().size());
    }

    @Test
    void shouldHandleNullRoles() {
        TenantId tenantId = TenantId.of("test-tenant-123");

        AuthContext context = new AuthContext(tenantId, null);

        assertTrue(context.getTenantId().isPresent());
        assertTrue(context.getRoles().isEmpty());
    }

    @Test
    void shouldReturnCorrectHasRole() {
        AuthContext context = new AuthContext(
            TenantId.of("tenant-123"),
            Set.of("ADMIN", "TEACHER")
        );

        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasRole("TEACHER"));
        assertFalse(context.hasRole("STUDENT"));
    }

    @Test
    void shouldReturnCorrectIsTenant() {
        TenantId tenantId = TenantId.of("tenant-123");
        AuthContext context = new AuthContext(tenantId, Set.of());

        assertTrue(context.isTenant(TenantId.of("tenant-123")));
        assertFalse(context.isTenant(TenantId.of("other-tenant")));
    }

    @Test
    void shouldReturnEmptyOptionalForNullTenantWhenCheckingIsTenant() {
        AuthContext context = new AuthContext(null, Set.of());

        assertFalse(context.isTenant(TenantId.of("any-tenant")));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=AuthContextTest`
Expected: FAIL with "class AuthContext not found"

**Step 3: Write minimal implementation**

Create file `src/main/java/com/k12/infrastructure/security/AuthContext.java`:

```java
package com.k12.infrastructure.security;

import com.k12.common.domain.model.TenantId;
import jakarta.enterprise.context.RequestScoped;

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
 */
@RequestScoped
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
        this.roles = roles != null ? roles : Set.of();
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
        return roles.contains(role);
    }

    /**
     * Checks if the current context belongs to a specific tenant.
     *
     * @param id The tenant ID to compare against
     * @return true if this context belongs to the specified tenant
     */
    public boolean isTenant(TenantId id) {
        return tenantId.isPresent() && tenantId.get().equals(id);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=AuthContextTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/infrastructure/security/AuthContext.java \
        src/test/java/com/k12/infrastructure/security/AuthContextTest.java
git commit -m "feat: add AuthContext request-scoped bean

Add @RequestScoped CDI bean to provide convenient access to
tenantId and roles from JWT tokens throughout the application.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 2: Enhance JWTPrincipal with TenantId Value Object

**Files:**
- Modify: `src/main/java/com/k12/infrastructure/security/JWTPrincipal.java`
- Test: `src/test/java/com/k12/infrastructure/security/JWTPrincipalTest.java`

**Step 1: Write the failing test**

Create/modify test file `src/test/java/com/k12/infrastructure/security/JWTPrincipalTest.java`:

```java
package com.k12.infrastructure.security;

import com.k12.common.domain.model.TenantId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JWTPrincipalTest {

    @Test
    void shouldCreateJWTPrincipalWithTenantId() {
        TenantId tenantId = TenantId.of("tenant-123");
        JWTPrincipal principal = new JWTPrincipal(
            "user-123",
            "user@example.com",
            Set.of("ADMIN"),
            tenantId
        );

        assertEquals("user-123", principal.getUserId());
        assertEquals("user@example.com", principal.getEmail());
        assertEquals("user-123", principal.getName());
        assertEquals(tenantId, principal.getTenantId());
        assertTrue(principal.hasRole("ADMIN"));
    }

    @Test
    void shouldHandleNullTenantId() {
        JWTPrincipal principal = new JWTPrincipal(
            "user-123",
            "user@example.com",
            Set.of("ADMIN"),
            null
        );

        assertEquals("user-123", principal.getUserId());
        assertNull(principal.getTenantId());
    }

    @Test
    void shouldHandleNullRoles() {
        TenantId tenantId = TenantId.of("tenant-123");
        JWTPrincipal principal = new JWTPrincipal(
            "user-123",
            "user@example.com",
            null,
            tenantId
        );

        assertTrue(principal.getRoles().isEmpty());
    }

    @Test
    void shouldReturnTenantIdValueObject() {
        TenantId tenantId = TenantId.of("tenant-456");
        JWTPrincipal principal = new JWTPrincipal(
            "user-123",
            "user@example.com",
            Set.of(),
            tenantId
        );

        assertNotNull(principal.getTenantId());
        assertTrue(principal.getTenantId() instanceof TenantId);
        assertEquals("tenant-456", principal.getTenantId().value());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=JWTPrincipalTest`
Expected: FAIL with constructor mismatch or type errors (current code expects String, not TenantId)

**Step 3: Write minimal implementation**

Modify file `src/main/java/com/k12/infrastructure/security/JWTPrincipal.java`:

Change the tenantId field and constructor:

```java
// In the imports section, add:
import com.k12.common.domain.model.TenantId;

// In the class, change:
private final String tenantId;

// To:
private final TenantId tenantId;

// Update the constructor signature:
public JWTPrincipal(String userId, String email, Set<String> roles, String tenantId) {

// To:
public JWTPrincipal(String userId, String email, Set<String> roles, TenantId tenantId) {

// Update the getTenantId() method:
public String getTenantId() {
    return tenantId;
}

// To:
public TenantId getTenantId() {
    return tenantId;
}

// Update the toString() method, change:
+ ", tenantId='" + tenantId + '\''

// To:
+ ", tenantId=" + tenantId
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=JWTPrincipalTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/infrastructure/security/JWTPrincipal.java \
        src/test/java/com/k12/infrastructure/security/JWTPrincipalTest.java
git commit -m "refactor: use TenantId value object in JWTPrincipal

Change JWTPrincipal to use type-safe TenantId value object instead
of raw String for better domain modeling and null-safety.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 3: Update JWTSecurityContext Constructor

**Files:**
- Modify: `src/main/java/com/k12/infrastructure/security/JWTSecurityContext.java`

**Step 1: Read the file to understand current implementation**

Read: `src/main/java/com/k12/infrastructure/security/JWTSecurityContext.java`

Note: This class needs to accept TenantId instead of String to match JWTPrincipal changes.

**Step 2: Update JWTSecurityContext**

Modify file `src/main/java/com/k12/infrastructure/security/JWTSecurityContext.java`:

```java
// In the imports section, add:
import com.k12.common.domain.model.TenantId;

// Find the constructor and update it:
public JWTSecurityContext(String userId, String email, Set<String> roles, String tenantId, URI uri) {

// Change to:
public JWTSecurityContext(String userId, String email, Set<String> roles, TenantId tenantId, URI uri) {

// The field and principal creation should already use the parameter correctly,
// so no other changes needed if it passes the tenantId to JWTPrincipal directly
```

**Step 3: Verify compilation**

Run: `./mvnw compile`
Expected: SUCCESS

**Step 4: Run existing tests to ensure no breakage**

Run: `./mvnw test -Dtest=JWTAuthenticationFilterTest`
Expected: Current tests should compile but may fail (we'll fix in next task)

**Step 5: Commit**

```bash
git add src/main/java/com/k12/infrastructure/security/JWTSecurityContext.java
git commit -m "refactor: update JWTSecurityContext to use TenantId

Update constructor to accept TenantId value object matching
JWTPrincipal changes.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 4: Update JWTAuthenticationFilter to Convert String to TenantId

**Files:**
- Modify: `src/main/java/com/k12/infrastructure/security/JWTAuthenticationFilter.java`
- Test: `src/test/java/com/k12/infrastructure/security/JWTAuthenticationFilterTest.java`

**Step 1: Add imports and helper method**

Add to imports in `JWTAuthenticationFilter.java`:

```java
import com.k12.common.domain.model.TenantId;
```

**Step 2: Add conversion method after the `getStringClaimSafely` method**

```java
/**
 * Safely extracts and converts tenantId claim to TenantId value object.
 *
 * @param claims JWT claims
 * @return TenantId or null if not present or invalid
 */
private TenantId extractTenantId(JwtClaims claims) {
    try {
        String tenantIdString = getStringClaimSafely(claims, "tenantId");
        if (tenantIdString == null || tenantIdString.isBlank()) {
            return null;
        }
        return TenantId.of(tenantIdString);
    } catch (IllegalArgumentException e) {
        LOGGER.warn("Invalid tenantId format in JWT: {}", e.getMessage());
        return null;
    }
}
```

**Step 3: Update the filter() method to use TenantId**

Find the section where JWTSecurityContext is created (around line 63-68) and update:

```java
// Old code:
JWTSecurityContext securityContext = new JWTSecurityContext(
        claims.getSubject(),
        getStringClaimSafely(claims, "email"),
        extractRoles(claims),
        getStringClaimSafely(claims, "tenantId"),
        requestContext.getUriInfo().getRequestUri());

// New code:
TenantId tenantId = extractTenantId(claims);
Set<String> roles = extractRoles(claims);

JWTSecurityContext securityContext = new JWTSecurityContext(
        claims.getSubject(),
        getStringClaimSafely(claims, "email"),
        roles,
        tenantId,
        requestContext.getUriInfo().getRequestUri());
```

**Step 4: Create and set AuthContext**

After setting the security context, add AuthContext creation:

```java
// Set security context on request
requestContext.setSecurityContext(securityContext);

// Create and store AuthContext for CDI access
AuthContext authContext = new AuthContext(tenantId, roles);

// Store claims in request context property for CDI access
requestContext.setProperty("jwt.claims", claims);

// NEW: Set additional properties for direct access
requestContext.setProperty("tenantId", tenantId);
requestContext.setProperty("roles", roles);
requestContext.setProperty("auth.context", authContext);
```

**Step 5: Run existing tests**

Run: `./mvnw test -Dtest=JWTAuthenticationFilterTest`
Expected: Tests should pass (verify filter still works)

**Step 6: Commit**

```bash
git add src/main/java/com/k12/infrastructure/security/JWTAuthenticationFilter.java
git commit -m "feat: extract TenantId and create AuthContext in JWT filter

- Convert String tenantId to TenantId value object
- Create AuthContext bean for CDI injection
- Set tenantId, roles, and auth.context as request properties
- Handle invalid tenantId format gracefully

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 5: Add Integration Test for AuthContext Access

**Files:**
- Modify: `src/main/java/com/k12/user/infrastructure/rest/resource/SecurityTestResource.java`
- Test: `src/test/java/com/k12/user/infrastructure/rest/resource/SecurityTestResourceTest.java` (if exists)

**Step 1: Add test endpoint to SecurityTestResource**

Add new method to `src/main/java/com/k12/user/infrastructure/rest/resource/SecurityTestResource.java`:

```java
import com.k12.infrastructure.security.AuthContext;
import com.k12.common.domain.model.TenantId;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Set;

// Add new endpoint:
@GET
@Path("/test-auth-context")
public Response testAuthContext() {
    // Test CDI injection
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
public Response testAllAccessPatterns(@Context SecurityContext securityContext,
                                      @Context ContainerRequestContext requestContext) {
    Map<String, Object> response = new HashMap<>();

    // Pattern 1: CDI injection
    response.put("cdi_tenantId", authContext.getTenantId().map(TenantId::value).orElse(null));
    response.put("cdi_roles", authContext.getRoles());

    // Pattern 2: SecurityContext
    JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
    if (principal != null) {
        response.put("sc_tenantId", principal.getTenantId() != null ? principal.getTenantId().value() : null);
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
```

Add field at class level:

```java
@Inject
private AuthContext authContext;
```

**Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: SUCCESS

**Step 3: Run integration test manually (optional)**

```bash
# Start the application
./mvnw quarkus:dev

# In another terminal, test the endpoint:
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@k12.com","password":"admin123"}' | jq -r '.token')

curl -X GET http://localhost:8080/api/security/test-auth-context \
  -H "Authorization: Bearer $TOKEN" | jq

curl -X GET http://localhost:8080/api/security/test-all-patterns \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Step 4: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/rest/resource/SecurityTestResource.java
git commit -m "feat: add test endpoints for AuthContext access patterns

Add endpoints to demonstrate and test CDI injection of AuthContext
and all three access patterns (CDI, SecurityContext, properties).

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 6: Update JWTAuthenticationFilterTest

**Files:**
- Modify: `src/test/java/com/k12/infrastructure/security/JWTAuthenticationFilterTest.java`

**Step 1: Read existing test to understand structure**

Read: `src/test/java/com/k12/infrastructure/security/JWTAuthenticationFilterTest.java`

**Step 2: Add new test for TenantId conversion**

Add test method to verify TenantId is created:

```java
@Test
void shouldExtractTenantIdAsValueObject() throws Exception {
    // Create mock JWT with tenantId
    String token = createMockJWT("user-123", "user@example.com", "ADMIN", "tenant-456");

    ContainerRequestContext requestContext = mockRequestContext(token);
    filter.filter(requestContext);

    // Verify SecurityContext has TenantId value object
    SecurityContext securityContext = requestContext.getSecurityContext();
    assertNotNull(securityContext);
    JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();
    assertNotNull(principal);
    assertNotNull(principal.getTenantId());
    assertTrue(principal.getTenantId() instanceof TenantId);
    assertEquals("tenant-456", principal.getTenantId().value());
}

@Test
void shouldSetAuthContextProperty() throws Exception {
    String token = createMockJWT("user-123", "user@example.com", "ADMIN", "tenant-456");

    ContainerRequestContext requestContext = mockRequestContext(token);
    filter.filter(requestContext);

    // Verify auth.context property is set
    AuthContext authContext = (AuthContext) requestContext.getProperty("auth.context");
    assertNotNull(authContext);
    assertTrue(authContext.getTenantId().isPresent());
    assertEquals("tenant-456", authContext.getTenantId().get().value());
    assertTrue(authContext.hasRole("ADMIN"));
}

@Test
void shouldSetTenantIdAndRolesAsProperties() throws Exception {
    String token = createMockJWT("user-123", "user@example.com", "ADMIN,TEACHER", "tenant-789");

    ContainerRequestContext requestContext = mockRequestContext(token);
    filter.filter(requestContext);

    // Verify tenantId property
    TenantId tenantId = (TenantId) requestContext.getProperty("tenantId");
    assertNotNull(tenantId);
    assertEquals("tenant-789", tenantId.value());

    // Verify roles property
    @SuppressWarnings("unchecked")
    Set<String> roles = (Set<String>) requestContext.getProperty("roles");
    assertNotNull(roles);
    assertEquals(2, roles.size());
    assertTrue(roles.contains("ADMIN"));
    assertTrue(roles.contains("TEACHER"));
}

@Test
void shouldHandleMissingTenantIdGracefully() throws Exception {
    // Create JWT without tenantId
    String token = createMockJWT("user-123", "user@example.com", "ADMIN", null);

    ContainerRequestContext requestContext = mockRequestContext(token);
    filter.filter(requestContext);

    SecurityContext securityContext = requestContext.getSecurityContext();
    JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

    assertNull(principal.getTenantId());

    AuthContext authContext = (AuthContext) requestContext.getProperty("auth.context");
    assertTrue(authContext.getTenantId().isEmpty());
}
```

**Step 3: Update helper method to include tenantId**

Find the `createMockJWT` helper method and update it to accept tenantId parameter:

```java
private String createMockJWT(String userId, String email, String roles, String tenantId) throws Exception {
    // Update to include tenantId in claims
    // Existing implementation - just add tenantId parameter
    JwtClaims claims = new JwtClaims();
    claims.setSubject(userId);
    claims.setStringClaim("email", email);
    claims.setStringClaim("roles", roles);
    if (tenantId != null) {
        claims.setStringClaim("tenantId", tenantId);
    }
    // ... rest of JWT generation
}
```

**Step 4: Run tests**

Run: `./mvnw test -Dtest=JWTAuthenticationFilterTest`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add src/test/java/com/k12/infrastructure/security/JWTAuthenticationFilterTest.java
git commit -m "test: add tests for TenantId and AuthContext in JWT filter

Verify that:
- TenantId value object is created from JWT
- AuthContext bean is created and stored
- Request properties are set correctly
- Missing tenantId is handled gracefully

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 7: Update Documentation

**Files:**
- Modify: `docs/JWT_AUTH_FILTER.md`

**Step 1: Read current documentation**

Read: `docs/JWT_AUTH_FILTER.md`

**Step 2: Add new section "Access Patterns"**

Add after "Usage" section:

```markdown
### Pattern 1: CDI Injection (Recommended)

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

### Pattern 2: SecurityContext (Enhanced)

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

### Pattern 3: Request Properties

Direct access from ContainerRequestContext:

```java
@GET
public Response getData(@Context ContainerRequestContext requestContext) {
    TenantId tenantId = (TenantId) requestContext.getProperty("tenantId");
    Set<String> roles = (Set<String>) requestContext.getProperty("roles");

    return Response.ok().build();
}
```
```

**Step 3: Update "JWTPrincipal" section**

Update the bullet points:

```markdown
### 3. JWTPrincipal
**Location:** `com.k12.infrastructure.security.JWTPrincipal`

Principal implementation containing:
- `getUserId()` - User ID from JWT subject
- `getEmail()` - User email from JWT claim
- `getRoles()` - Set of user roles
- `getTenantId()` - **TenantId value object** for multi-tenancy (changed from String)
- `hasRole(String)` - Check if user has specific role
```

**Step 4: Update code examples**

Update all examples that use `getTenantId()` to handle `TenantId` instead of `String`.

**Step 5: Commit**

```bash
git add docs/JWT_AUTH_FILTER.md
git commit -m "docs: update JWT auth filter documentation

Document new AuthContext CDI bean and enhanced access patterns.
Update examples to show TenantId value object usage.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 8: Final Integration Test

**Files:**
- Run all tests

**Step 1: Run full test suite**

Run: `./mvnw test`
Expected: All tests PASS

**Step 2: Check for compilation warnings**

Run: `./mvnw compile`
Expected: SUCCESS with no warnings

**Step 3: Run application and test manually**

```bash
./mvnw quarkus:dev

# Test login and JWT generation
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@k12.com","password":"admin123"}' | jq -r '.token')

# Test all access patterns
curl -X GET http://localhost:8080/api/security/test-all-patterns \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected output shows all three patterns return same data:
# {
#   "cdi_tenantId": "...",
#   "cdi_roles": ["SUPER_ADMIN"],
#   "sc_tenantId": "...",
#   "sc_roles": ["SUPER_ADMIN"],
#   "prop_tenantId": "...",
#   "prop_roles": ["SUPER_ADMIN"]
# }
```

**Step 4: Final commit**

```bash
git add .
git commit -m "feat: complete JWT tenant context enhancement

All implementation complete:
- AuthContext CDI bean for injection
- TenantId value object in JWTPrincipal
- Multiple access patterns available
- Full test coverage
- Documentation updated

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Verification Checklist

Before considering this feature complete, verify:

- [ ] All tests pass: `./mvnw test`
- [ ] No compilation warnings: `./mvnw compile`
- [ ] Manual testing confirms all three access patterns work
- [ ] Documentation updated in `docs/JWT_AUTH_FILTER.md`
- [ ] Test endpoints demonstrate usage
- [ ] Thread safety verified (request-scoped, no shared state)
- [ ] Error handling tested (missing tenantId, invalid format)

---

## Next Steps After Implementation

1. **Update existing code**: Search for usages of `JWTPrincipal.getTenantId()` expecting String and update
2. **Add @TenantRequired**: Consider adding annotation for endpoints requiring tenant context
3. **Repository filtering**: Add automatic tenant filtering at repository layer
4. **Monitor performance**: Verify no performance impact from TenantId conversion

---

**Estimated Implementation Time:** 2-3 hours
**Number of Commits:** 8 (following TDD and frequent commits)
**Test Coverage:** All new code fully tested
