# Authentication and JWT Login Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a login endpoint that generates JWT tokens with user roles, following full Event Sourcing and DDD patterns.

**Architecture:** Event-sourced UserRepository using jOOQ and Kryo serialization, domain service for credential validation, application service for orchestration, REST endpoint for JWT delivery. Consistent with existing Tenant module patterns.

**Tech Stack:** Quarkus, JAX-RS, SmallRye JWT (RS256), jOOQ, PostgreSQL, BCrypt, Kryo serialization

---

## Prerequisites

Before starting, ensure:
- Database has `users` and `user_events` tables (created in previous session)
- RSA keys exist in `src/main/resources/keys/`
- Test database is running on localhost:5432
- SUPER_ADMIN user exists: admin@k12.com / admin123

---

## Task 1: Add tenant_id to users table

**Why:** Users need to be associated with tenants for multi-tenancy support.

**Files:**
- Create: `src/main/resources/db/migration/V5__Add_Tenant_ID_To_Users.sql`

**Step 1: Create migration file**

```bash
cat > src/main/resources/db/migration/V5__Add_Tenant_ID_To_Users.sql << 'EOF'
-- Add tenant_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Add index for tenant_id lookups
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

-- Add foreign key constraint to tenants table
ALTER TABLE users
ADD CONSTRAINT fk_users_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;

COMMENT ON COLUMN users.tenant_id IS 'Associated tenant ID for multi-tenancy support';
EOF
```

**Step 2: Run migration**

```bash
./mvnw quarkus:dev
```
Expected: Flyway runs migration automatically on startup. Check logs for "Successfully applied V5__Add_Tenant_ID_To_Users.sql".

**Step 3: Verify migration**

```bash
PGPASSWORD=k12_password psql -h localhost -p 15432 -U k12_user -d k12_db -c "\d users"
```
Expected: Should see `tenant_id | uuid | | |` column.

**Step 4: Update existing user with tenant_id**

```bash
PGPASSWORD=k12_password psql -h localhost -p 15432 -U k12_user -d k12_db -c "UPDATE users SET tenant_id = (SELECT id FROM tenants LIMIT 1) WHERE email = 'admin@k12.com';"
```

**Step 5: Commit**

```bash
git add src/main/resources/db/migration/V5__Add_Tenant_ID_To_Users.sql
git commit -m "feat: add tenant_id to users table for multi-tenancy

Add tenant_id column with foreign key to tenants table.
Include index for query optimization.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Create AuthenticationError domain error

**Why:** Centralized error handling for authentication failures.

**Files:**
- Create: `src/main/java/com/k12/user/domain/error/AuthenticationError.java`

**Step 1: Write the error interface**

```bash
cat > src/main/java/com/k12/user/domain/error/AuthenticationError.java << 'EOF'
package com.k12.user.domain.error;

/**
 * Domain errors for authentication operations.
 * Following Result Either Pattern (ROP) for error handling.
 */
public sealed interface AuthenticationError {

    String message();

    /**
     * Invalid email or password combination.
     */
    record InvalidCredentials(String message) implements AuthenticationError {
        public InvalidCredentials() {
            this("Invalid email or password");
        }
    }

    /**
     * No account found with the provided email.
     */
    record UserNotFound(String message) implements AuthenticationError {
        public UserNotFound() {
            this("No account found with this email");
        }
    }

    /**
     * User account has been suspended.
     */
    record UserSuspended(String message) implements AuthenticationError {
        public UserSuspended() {
            this("Account has been suspended. Please contact support.");
        }
    }

    /**
     * User account is not active.
     */
    record UserInactive(String message) implements AuthenticationError {
        public UserInactive() {
            this("Account is not active. Please contact support.");
        }
    }
}
EOF
```

**Step 2: Verify compilation**

```bash
./mvnw compile
```
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/k12/user/domain/error/AuthenticationError.java
git commit -m "feat: add AuthenticationError domain error types

Add sealed interface for authentication error handling.
Includes InvalidCredentials, UserNotFound, UserSuspended, UserInactive.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Create PasswordMatcher utility

**Why:** Domain service needs BCrypt password verification.

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/security/PasswordMatcher.java`
- Create: `src/test/java/com/k12/user/infrastructure/security/PasswordMatcherTest.java`

**Step 1: Write the failing test**

```bash
cat > src/test/java/com/k12/user/infrastructure/security/PasswordMatcherTest.java << 'EOF'
package com.k12.user.infrastructure.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordMatcherTest {

    @Test
    void shouldVerifyCorrectPassword() {
        // This is a known BCrypt hash for "password123"
        String hash = "$2a$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6";
        String password = "password123";

        assertTrue(PasswordMatcher.verify(password, hash));
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String hash = "$2a$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6";
        String password = "wrongpassword";

        assertFalse(PasswordMatcher.verify(password, hash));
    }

    @Test
    void shouldHandleNullHash() {
        String password = "password123";

        assertFalse(PasswordMatcher.verify(password, null));
    }
}
EOF
```

**Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=PasswordMatcherTest
```
Expected: FAIL with "class PasswordMatcher not found"

**Step 3: Write minimal implementation**

```bash
cat > src/main/java/com/k12/user/infrastructure/security/PasswordMatcher.java << 'EOF'
package com.k12.user.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility for verifying BCrypt password hashes.
 */
public final class PasswordMatcher {

    private PasswordMatcher() {
        // Utility class
    }

    /**
     * Verify a password against a BCrypt hash.
     *
     * @param password Plain text password
     * @param hash BCrypt hash
     * @return true if password matches hash
     */
    public static boolean verify(String password, String hash) {
        if (hash == null || password == null) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
            return result.verified;
        } catch (Exception e) {
            return false;
        }
    }
}
EOF
```

**Note:** This uses `at.favre.lib.crypto.bcrypt` which should already be available. If not, add to pom.xml:
```xml
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```

**Step 4: Run test to verify it passes**

```bash
./mvnw test -Dtest=PasswordMatcherTest
```
Expected: PASS (all 3 tests)

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/security/PasswordMatcher.java \
        src/test/java/com/k12/user/infrastructure/security/PasswordMatcherTest.java
git commit -m "feat: add PasswordMatcher utility for BCrypt verification

Add utility class for verifying BCrypt password hashes.
Includes tests for correct/incorrect passwords and null handling.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Create TokenService for JWT generation

**Why:** Generate signed JWT tokens with user claims.

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/security/TokenService.java`
- Create: `src/test/java/com/k12/user/infrastructure/security/TokenServiceTest.java`
- Read: `src/main/java/com/k12/user/domain/models/User.java`

**Step 1: Write the failing test**

```bash
cat > src/test/java/com/k12/user/infrastructure/security/TokenServiceTest.java << 'EOF'
package com.k12.user.infrastructure.security;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

class TokenServiceTest {

    @Test
    void shouldGenerateTokenWithRequiredClaims() {
        User user = createTestUser();
        String tenantId = "test-tenant-123";

        String token = TokenService.generateToken(user, tenantId);

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ")); // JWT format
    }

    @Test
    void shouldIncludeUserIdInToken() {
        User user = createTestUser();
        String tenantId = "test-tenant-123";

        String token = TokenService.generateToken(user, tenantId);

        // Decode and verify sub claim contains userId
        String userId = TokenService.extractClaim(token, "sub");
        assertEquals(user.userId().value(), userId);
    }

    @Test
    void shouldIncludeEmailInToken() {
        User user = createTestUser();
        String tenantId = "test-tenant-123";

        String token = TokenService.generateToken(user, tenantId);

        String email = TokenService.extractClaim(token, "email");
        assertEquals(user.emailAddress().value(), email);
    }

    @Test
    void shouldIncludeRolesInToken() {
        User user = createTestUser();
        String tenantId = "test-tenant-123";

        String token = TokenService.generateToken(user, tenantId);

        String roles = TokenService.extractClaim(token, "roles");
        assertNotNull(roles);
    }

    private User createTestUser() {
        return new User(
            new UserId("550e8400-e29b-41d4-a716-446655440000"),
            new EmailAddress("test@example.com"),
            new PasswordHash("$2a$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6"),
            Set.of(UserRole.SUPER_ADMIN),
            UserStatus.ACTIVE,
            new UserName("Test User")
        );
    }
}
EOF
```

**Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=TokenServiceTest
```
Expected: FAIL with "class TokenService not found"

**Step 3: Write minimal implementation**

```bash
cat > src/main/java/com/k12/user/infrastructure/security/TokenService.java << 'EOF'
package com.k12.user.infrastructure.security;

import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Collectors;

import io.smallrye.jwt.build.Jwt;

/**
 * Service for generating JWT tokens.
 */
@ApplicationScoped
public class TokenService {

    private static final String ISSUER = "k12-api";
    private static final long TOKEN_VALIDITY_HOURS = 24;

    /**
     * Generate a JWT token for the given user.
     *
     * @param user The user to generate token for
     * @param tenantId The tenant ID
     * @return Signed JWT token string
     */
    public static String generateToken(User user, String tenantId) {
        Instant now = Instant.now();
        Instant exp = now.plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS);

        String roles = user.userRole().stream()
            .map(UserRole::name)
            .collect(Collectors.joining(","));

        return Jwt.builder()
            .subject(user.userId().value())
            .claim("email", user.emailAddress().value())
            .claim("roles", roles)
            .claim("tenantId", tenantId)
            .issuedAt(now)
            .expiresAt(exp)
            .issuer(ISSUER)
            .jws()
            .keyId("1") // Matches configured key
            .sign(getPrivateKey());
    }

    /**
     * Extract a claim from a JWT token for testing purposes.
     * In production, claims are validated by SmallRye JWT.
     */
    public static String extractClaim(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // Simple JSON parsing for testing
            String search = "\"" + claimName + "\":\"";
            int start = payload.indexOf(search);
            if (start == -1) return null;
            start += search.length();
            int end = payload.indexOf("\"", start);
            if (end == -1) return null;
            return payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static PrivateKey getPrivateKey() {
        try {
            String keyContent = new String(Files.readAllBytes(
                Paths.get("src/main/resources/keys/private-key.pem")), StandardCharsets.UTF_8);

            // Remove PEM headers and footers
            keyContent = keyContent.replace("-----BEGIN PRIVATE KEY-----", "")
                                  .replace("-----END PRIVATE KEY-----", "")
                                  .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
        }
    }
}
EOF
```

**Step 4: Run test to verify it passes**

```bash
./mvnw test -Dtest=TokenServiceTest
```
Expected: PASS (all 4 tests)

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/security/TokenService.java \
        src/test/java/com/k12/user/infrastructure/security/TokenServiceTest.java
git commit -m "feat: add TokenService for JWT generation

Add service to generate RS256 JWT tokens with user claims.
Includes userId, email, roles, tenantId, and standard JWT claims.
Uses private-key.pem for signing, 24-hour expiration.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Create AuthenticationService domain service

**Why:** Domain service for credential validation logic.

**Files:**
- Create: `src/main/java/com/k12/user/domain/service/AuthenticationService.java`
- Create: `src/test/java/com/k12/user/domain/service/AuthenticationServiceTest.java`
- Modify: `src/main/java/com/k12/user/domain/ports/out/UserRepository.java` (verify interface)

**Step 1: Verify UserRepository interface**

```bash
cat src/main/java/com/k12/user/domain/ports/out/UserRepository.java | grep -A 3 "findByEmailAddress"
```
Expected: Should see `Optional<User> findByEmailAddress(String emailAddress);`

**Step 2: Write the failing test**

```bash
cat > src/test/java/com/k12/user/domain/service/AuthenticationServiceTest.java << 'EOF'
package com.k12.user.domain.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.ports.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

class AuthenticationServiceTest {

    private UserRepository userRepository;
    private AuthenticationService authenticationService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authenticationService = new AuthenticationService(userRepository);

        activeUser = new User(
            new UserId("550e8400-e29b-41d4-a716-446655440000"),
            new EmailAddress("admin@k12.com"),
            new PasswordHash("$2a$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6"),
            Set.of(UserRole.SUPER_ADMIN),
            UserStatus.ACTIVE,
            new UserName("Admin User")
        );
    }

    @Test
    void shouldAuthenticateUserWithValidCredentials() {
        String email = "admin@k12.com";
        String password = "password123"; // This hash matches this password

        when(userRepository.findByEmailAddress(email))
            .thenReturn(Optional.of(activeUser));

        Result<User, AuthenticationError> result =
            authenticationService.authenticate(email, password);

        assertTrue(result.isSuccess());
        assertEquals(activeUser, result.get());
    }

    @Test
    void shouldFailWithInvalidCredentials() {
        String email = "admin@k12.com";
        String password = "wrongpassword";

        when(userRepository.findByEmailAddress(email))
            .thenReturn(Optional.of(activeUser));

        Result<User, AuthenticationError> result =
            authenticationService.authenticate(email, password);

        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.InvalidCredentials);
    }

    @Test
    void shouldFailWhenUserNotFound() {
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findByEmailAddress(email))
            .thenReturn(Optional.empty());

        Result<User, AuthenticationError> result =
            authenticationService.authenticate(email, password);

        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.UserNotFound);
    }

    @Test
    void shouldFailWhenUserSuspended() {
        User suspendedUser = new User(
            activeUser.userId(),
            activeUser.emailAddress(),
            activeUser.passwordHash(),
            activeUser.userRole(),
            UserStatus.SUSPENDED,
            activeUser.name()
        );

        String email = "admin@k12.com";
        String password = "password123";

        when(userRepository.findByEmailAddress(email))
            .thenReturn(Optional.of(suspendedUser));

        Result<User, AuthenticationError> result =
            authenticationService.authenticate(email, password);

        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.UserSuspended);
    }
}
EOF
```

**Step 3: Run test to verify it fails**

```bash
./mvnw test -Dtest=AuthenticationServiceTest
```
Expected: FAIL with "class AuthenticationService not found"

**Step 4: Write minimal implementation**

```bash
cat > src/main/java/com/k12/user/domain/service/AuthenticationService.java << 'EOF'
package com.k12.user.domain.service;

import com.k12.common.domain.model.Result;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.security.PasswordMatcher;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Domain service for authenticating users.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    /**
     * Authenticate a user with email and password.
     *
     * @param email User's email address
     * @param password Plain text password
     * @return Result containing User on success, AuthenticationError on failure
     */
    public Result<User, AuthenticationError> authenticate(String email, String password) {
        // Find user by email
        return userRepository.findByEmailAddress(email)
            .map(user -> validateUser(user, password))
            .orElse(Result.failure(new AuthenticationError.UserNotFound()));
    }

    private Result<User, AuthenticationError> validateUser(User user, String password) {
        // Check user status
        if (user.status() == UserStatus.SUSPENDED) {
            return Result.failure(new AuthenticationError.UserSuspended());
        }

        if (user.status() != UserStatus.ACTIVE) {
            return Result.failure(new AuthenticationError.UserInactive());
        }

        // Verify password
        if (!PasswordMatcher.verify(password, user.passwordHash().value())) {
            return Result.failure(new AuthenticationError.InvalidCredentials());
        }

        return Result.success(user);
    }
}
EOF
```

**Step 5: Run test to verify it passes**

```bash
./mvnw test -Dtest=AuthenticationServiceTest
```
Expected: PASS (all 4 tests)

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/domain/service/AuthenticationService.java \
        src/test/java/com/k12/user/domain/service/AuthenticationServiceTest.java
git commit -m "feat: add AuthenticationService domain service

Add domain service for user credential validation.
Handles email lookup, password verification, and status checks.
Follows Result Either Pattern for error handling.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Create UserRepositoryImpl with event sourcing

**Why:** Implement UserRepository to load users from database using event sourcing.

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/persistence/UserRepositoryImpl.java`
- Create: `src/test/java/com/k12/user/infrastructure/persistence/UserRepositoryImplIntegrationTest.java`
- Read: `src/main/java/com/k12/tenant/infrastructure/persistence/TenantRepositoryImpl.java` (reference)

**Step 1: Write the failing integration test**

```bash
cat > src/test/java/com/k12/user/infrastructure/persistence/UserRepositoryImplIntegrationTest.java << 'EOF'
package com.k12.user.infrastructure.persistence;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.ports.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import java.util.Optional;

@QuarkusTest
class UserRepositoryImplIntegrationTest {

    @Inject
    UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        Optional<User> user = userRepository.findByEmailAddress("admin@k12.com");

        assertTrue(user.isPresent());
        assertEquals("admin@k12.com", user.get().emailAddress().value());
        assertTrue(user.get().userRole().contains(UserRole.SUPER_ADMIN));
    }

    @Test
    void shouldReturnEmptyForNonExistentEmail() {
        Optional<User> user = userRepository.findByEmailAddress("nonexistent@example.com");

        assertTrue(user.isEmpty());
    }

    @Test
    void shouldFindUserById() {
        UserId userId = new UserId("550e8400-e29b-41d4-a716-446655440000");

        Optional<User> user = userRepository.findById(userId);

        assertTrue(user.isPresent());
        assertEquals(userId, user.get().userId());
    }
}
EOF
```

**Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=UserRepositoryImplIntegrationTest
```
Expected: FAIL with "No beans found" or similar injection error

**Step 3: Write UserRepositoryImpl implementation**

```bash
cat > src/main/java/com/k12/user/infrastructure/persistence/UserRepositoryImpl.java << 'EOF'
package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS;
import static com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS;

import com.k12.backend.infrastructure.jooq.public_.tables.records.UserEventsRecord;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserReconstructor;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.tenant.infrastructure.persistence.KryoEventSerializer;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserRepository using jOOQ with event sourcing.
 * Loads User aggregates from user_events table using Kryo serialization.
 * Mirrors TenantRepositoryImpl pattern.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final AgroalDataSource dataSource;

    @Override
    public User save(User user) {
        throw new UnsupportedOperationException("User saving not yet implemented");
    }

    @Override
    public Optional<User> findById(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            // Load events for this user
            List<com.k12.user.domain.models.events.UserEvents> events = loadEvents(ctx, UUID.fromString(userId.value()));

            if (events.isEmpty()) {
                return Optional.empty();
            }

            // Reconstruct user from events
            return UserReconstructor.reconstructWithValidation(events);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmailAddress(String emailAddress) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            // First, get user_id from users projection table
            UUID userId = ctx.select(USERS.ID)
                .from(USERS)
                .where(USERS.EMAIL.eq(emailAddress))
                .fetchOne(USERS.ID);

            if (userId == null) {
                return Optional.empty();
            }

            // Load events and reconstruct
            return findById(new UserId(userId.toString()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmail(String emailAddress) {
        return findByEmailAddress(emailAddress).isPresent();
    }

    @Override
    public void deleteById(UserId userId) {
        throw new UnsupportedOperationException("User deletion not yet implemented");
    }

    @Override
    public long count() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchCount(USERS);
    }

    /**
     * Load all events for a user from user_events table.
     */
    private List<com.k12.user.domain.models.events.UserEvents> loadEvents(DSLContext ctx, UUID userId) {
        try {
            List<UserEventsRecord> records = ctx.selectFrom(USER_EVENTS)
                .where(USER_EVENTS.USER_ID.eq(userId))
                .orderBy(USER_EVENTS.VERSION.asc())
                .fetch();

            List<com.k12.user.domain.models.events.UserEvents> events = new ArrayList<>();
            for (UserEventsRecord record : records) {
                byte[] eventData = record.getEventData();
                events.add(KryoEventSerializer.deserializeUserEvent(eventData));
            }

            return events;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
EOF
```

**Step 4: Update KryoEventSerializer to support User events**

```bash
# Read the existing KryoEventSerializer from tenant module
cat src/main/java/com/k12/tenant/infrastructure/persistence/KryoEventSerializer.java
```

You need to add support for User events. Add this method to KryoEventSerializer:

```java
/**
 * Deserialize a User event from Kryo format.
 */
public static com.k12.user.domain.models.events.UserEvents deserializeUserEvent(byte[] data) {
    try (Input input = new Input(data)) {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);

        // Register User event types
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserCreated.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserSuspended.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserActivated.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserEmailUpdated.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserPasswordUpdated.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserRoleAdded.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserRoleRemoved.class);
        kryo.register(com.k12.user.domain.models.events.UserEvents.UserNameUpdated.class);

        // Register value objects
        kryo.register(UserId.class);
        kryo.register(com.k12.user.domain.models.EmailAddress.class);
        kryo.register(com.k12.user.domain.models.PasswordHash.class);
        kryo.register(com.k12.user.domain.models.UserName.class);
        kryo.register(com.k12.user.domain.models.UserRole.class);
        kryo.register(com.k12.user.domain.models.UserStatus.class);
        kryo.register(ArrayList.class);

        return (com.k12.user.domain.models.events.UserEvents) kryo.readClassAndObject(input);
    } catch (Exception e) {
        throw new RuntimeException("Failed to deserialize User event", e);
    }
}
```

**Step 5: Run test to verify it passes**

```bash
./mvnw test -Dtest=UserRepositoryImplIntegrationTest
```
Expected: PASS (all 3 tests)

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/persistence/UserRepositoryImpl.java \
        src/test/java/com/k12/user/infrastructure/persistence/UserRepositoryImplIntegrationTest.java \
        src/main/java/com/k12/tenant/infrastructure/persistence/KryoEventSerializer.java
git commit -m "feat: implement UserRepository with event sourcing

Add UserRepositoryImpl using jOOQ and Kryo serialization.
Loads User aggregates from user_events table.
Update KryoEventSerializer to support User domain events.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Create AuthenticationApplicationService

**Why:** Application service orchestrates login flow and creates response DTOs.

**Files:**
- Create: `src/main/java/com/k12/user/application/AuthenticationApplicationService.java`
- Create: `src/main/java/com/k12/user/application/dto/LoginRequest.java`
- Create: `src/main/java/com/k12/user/application/dto/LoginResponse.java`
- Create: `src/test/java/com/k12/user/application/AuthenticationApplicationServiceTest.java`

**Step 1: Create DTOs**

```bash
# LoginRequest
cat > src/main/java/com/k12/user/application/dto/LoginRequest.java << 'EOF'
package com.k12.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 */
public record LoginRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    String password
) {}
EOF

# LoginResponse
cat > src/main/java/com/k12/user/application/dto/LoginResponse.java << 'EOF'
package com.k12.user.application.dto;

import com.k12.user.domain.models.UserRole;

import java.util.Set;

/**
 * Response DTO for successful login.
 */
public record LoginResponse(

    String token,
    UserInfo user
) {
    public record UserInfo(
        String id,
        String email,
        String name,
        Set<UserRole> roles
    ) {}

    public static LoginResponse from(String token, com.k12.user.domain.models.User user) {
        return new LoginResponse(
            token,
            new LoginResponse.UserInfo(
                user.userId().value(),
                user.emailAddress().value(),
                user.name().value(),
                user.userRole()
            )
        );
    }
}
EOF
```

**Step 2: Write the failing test**

```bash
cat > src/test/java/com/k12/user/application/AuthenticationApplicationServiceTest.java << 'EOF'
package com.k12.user.application;

import com.k12.common.domain.model.Result;
import com.k12.user.application.dto.LoginRequest;
import com.k12.user.application.dto.LoginResponse;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.User;
import com.k12.user.domain.service.AuthenticationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

class AuthenticationApplicationServiceTest {

    private AuthenticationService authenticationService;
    private AuthenticationApplicationService applicationService;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        applicationService = new AuthenticationApplicationService(authenticationService);
    }

    @Test
    void shouldReturnTokenOnValidLogin() {
        LoginRequest request = new LoginRequest("admin@k12.com", "password123");
        User mockUser = createMockUser();

        when(authenticationService.authenticate("admin@k12.com", "password123"))
            .thenReturn(Result.success(mockUser));

        Result<LoginResponse, AuthenticationError> result =
            applicationService.login(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.get().token());
        assertEquals("admin@k12.com", result.get().user().email());
    }

    @Test
    void shouldReturnErrorOnInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin@k12.com", "wrongpassword");

        when(authenticationService.authenticate("admin@k12.com", "wrongpassword"))
            .thenReturn(Result.failure(new AuthenticationError.InvalidCredentials()));

        Result<LoginResponse, AuthenticationError> result =
            applicationService.login(request);

        assertTrue(result.isFailure());
        assertTrue(result.getError() instanceof AuthenticationError.InvalidCredentials);
    }

    private User createMockUser() {
        return new User(
            new com.k12.common.domain.model.UserId("550e8400-e29b-41d4-a716-446655440000"),
            new com.k12.user.domain.models.EmailAddress("admin@k12.com"),
            new com.k12.user.domain.models.PasswordHash("$2a$12$hash"),
            Set.of(com.k12.user.domain.models.UserRole.SUPER_ADMIN),
            com.k12.user.domain.models.UserStatus.ACTIVE,
            new com.k12.user.domain.models.UserName("Admin")
        );
    }
}
EOF
```

**Step 3: Run test to verify it fails**

```bash
./mvnw test -Dtest=AuthenticationApplicationServiceTest
```
Expected: FAIL with "class AuthenticationApplicationService not found"

**Step 4: Write minimal implementation**

```bash
cat > src/main/java/com/k12/user/application/AuthenticationApplicationService.java << 'EOF'
package com.k12.user.application;

import com.k12.common.domain.model.Result;
import com.k12.user.application.dto.LoginRequest;
import com.k12.user.application.dto.LoginResponse;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.User;
import com.k12.user.domain.service.AuthenticationService;
import com.k12.user.infrastructure.security.TokenService;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Application service for authentication operations.
 * Orchestrates login flow and creates response DTOs.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class AuthenticationApplicationService {

    private final AuthenticationService authenticationService;

    /**
     * Authenticate user and return JWT token.
     *
     * @param request Login request with email and password
     * @return Result containing LoginResponse with token on success, AuthenticationError on failure
     */
    public Result<LoginResponse, AuthenticationError> login(LoginRequest request) {
        return authenticationService.authenticate(request.email(), request.password())
            .map(user -> {
                String tenantId = getTenantIdForUser(user);
                String token = TokenService.generateToken(user, tenantId);
                return LoginResponse.from(token, user);
            });
    }

    /**
     * Get tenant ID for user.
     * TODO: Load from user's tenant association when implemented.
     */
    private String getTenantIdForUser(User user) {
        // For now, use a default tenant ID
        // In the future, this should come from the User aggregate or users table
        return "default-tenant";
    }
}
EOF
```

**Step 5: Run test to verify it passes**

```bash
./mvnw test -Dtest=AuthenticationApplicationServiceTest
```
Expected: PASS (all 2 tests)

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/application/AuthenticationApplicationService.java \
        src/main/java/com/k12/user/application/dto/LoginRequest.java \
        src/main/java/com/k12/user/application/dto/LoginResponse.java \
        src/test/java/com/k12/user/application/AuthenticationApplicationServiceTest.java
git commit -m "feat: add AuthenticationApplicationService

Add application service to orchestrate login flow.
Creates LoginResponse with JWT token on successful authentication.
Includes LoginRequest and LoginResponse DTOs.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Create AuthResource REST endpoint

**Why:** Expose login endpoint via REST API.

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/rest/resource/AuthResource.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/LoginRequestDTO.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/LoginResponseDTO.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/mapper/AuthErrorResponseMapper.java`
- Create: `src/test/java/com/k12/user/infrastructure/rest/resource/AuthResourceIntegrationTest.java`

**Step 1: Create REST DTOs**

```bash
# LoginRequestDTO
cat > src/main/java/com/k12/user/infrastructure/rest/dto/LoginRequestDTO.java << 'EOF'
package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Login request with email and password")
public record LoginRequestDTO(

    @Schema(description = "User email address", example = "admin@k12.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Schema(description =User password", example = "admin123", required = true)
    @NotBlank(message = "Password is required")
    String password
) {}
EOF

# LoginResponseDTO
cat > src/main/java/com/k12/user/infrastructure/rest/dto/LoginResponseDTO.java << 'EOF'
package com.k12.user.infrastructure.rest.dto;

import com.k12.user.domain.models.UserRole;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Login response with JWT token and user info")
public record LoginResponseDTO(

    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,

    @Schema(description = "User information")
    UserInfoDTO user
) {
    @Schema(description = "User information")
    public record UserInfoDTO(
        @Schema(description = "User unique identifier")
        String id,

        @Schema(description = "User email address")
        String email,

        @Schema(description = "User full name")
        String name,

        @Schema(description = "User roles")
        Set<UserRole> roles
    ) {}

    static LoginResponseDTO from(String token, com.k12.user.application.dto.LoginResponse.UserInfo userInfo) {
        return new LoginResponseDTO(
            token,
            new UserInfoDTO(userInfo.id(), userInfo.email(), userInfo.name(), userInfo.roles())
        );
    }
}
EOF

# ErrorResponseDTO (reuse from tenant or create specific)
cat > src/main/java/com/k12/user/infrastructure/rest/dto/ErrorResponseDTO.java << 'EOF'
package com.k12.user.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response for authentication failures")
public record ErrorResponseDTO(

    @Schema(description = "Error code", example = "INVALID_CREDENTIALS")
    String error,

    @Schema(description = "Human-readable error message")
    String message,

    @Schema(description = "Timestamp when the error occurred")
    Instant timestamp
) {
    static ErrorResponseDTO from(com.k12.user.domain.error.AuthenticationError e) {
        return new ErrorResponseDTO(
            e.getClass().getSimpleName().toUpperCase().replace("AUTHENTICATIONERROR.", ""),
            e.message(),
            Instant.now()
        );
    }
}
EOF
```

**Step 2: Create error response mapper**

```bash
cat > src/main/java/com/k12/user/infrastructure/rest/mapper/AuthErrorResponseMapper.java << 'EOF'
package com.k12.user.infrastructure.rest.mapper;

import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class AuthErrorResponseMapper {

    public static Response toResponse(AuthenticationError error) {
        Status status = determineStatus(error);
        ErrorResponseDTO dto = ErrorResponseDTO.from(error);
        return Response.status(status).entity(dto).build();
    }

    private static Status determineStatus(AuthenticationError error) {
        if (error instanceof AuthenticationError.UserNotFound
            || error instanceof AuthenticationError.InvalidCredentials) {
            return Status.UNAUTHORIZED; // 401
        }
        if (error instanceof AuthenticationError.UserSuspended
            || error instanceof AuthenticationError.UserInactive) {
            return Status.FORBIDDEN; // 403
        }
        return Status.BAD_REQUEST; // 400
    }
}
EOF
```

**Step 3: Write the failing integration test**

```bash
cat > src/test/java/com/k12/user/infrastructure/rest/resource/AuthResourceIntegrationTest.java << 'EOF'
package com.k12.user.infrastructure.rest.resource;

import com.k12.user.infrastructure.rest.dto.LoginRequestDTO;
import com.k12.user.infrastructure.rest.dto.LoginResponseDTO;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
class AuthResourceIntegrationTest {

    @Test
    void shouldReturnTokenOnValidLogin() {
        LoginRequestDTO request = new LoginRequestDTO("admin@k12.com", "admin123");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/auth/login")
            .then()
            .extract()
            .response();

        assertEquals(200, response.statusCode());
        LoginResponseDTO dto = response.as(LoginResponseDTO.class);
        assertNotNull(dto.token());
        assertEquals("admin@k12.com", dto.user().email());
    }

    @Test
    void shouldReturn401OnInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("admin@k12.com", "wrongpassword");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/auth/login")
            .then()
            .extract()
            .response();

        assertEquals(401, response.statusCode());
        ErrorResponseDTO dto = response.as(ErrorResponseDTO.class);
        assertNotNull(dto.error());
    }

    @Test
    void shouldReturn401OnNonExistentEmail() {
        LoginRequestDTO request = new LoginRequestDTO("nonexistent@example.com", "password");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/auth/login")
            .then()
            .extract()
            .response();

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn400OnInvalidEmail() {
        String invalidRequest = "{\"email\":\"invalid-email\",\"password\":\"password\"}";

        Response response = given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .extract()
            .response();

        assertEquals(400, response.statusCode());
    }
}
EOF
```

**Step 4: Run test to verify it fails**

```bash
./mvnw test -Dtest=AuthResourceIntegrationTest
```
Expected: FAIL with 404 (endpoint doesn't exist)

**Step 5: Write AuthResource implementation**

```bash
cat > src/main/java/com/k12/user/infrastructure/rest/resource/AuthResource.java << 'EOF'
package com.k12.user.infrastructure.rest.resource;

import com.k12.user.application.AuthenticationApplicationService;
import com.k12.user.application.dto.LoginRequest;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;
import com.k12.user.infrastructure.rest.dto.LoginRequestDTO;
import com.k12.user.infrastructure.rest.dto.LoginResponseDTO;
import com.k12.user.infrastructure.rest.mapper.AuthErrorResponseMapper;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthResource {

    private final AuthenticationApplicationService authenticationService;

    @POST
    @Path("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user with email and password, returns JWT token"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
        ),
        @APIResponse(
            responseCode = "401",
            description = "Invalid credentials or user not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
    })
    public Response login(
        @Valid
        @RequestBody(description = "Login credentials", required = true)
        LoginRequestDTO request
    ) {
        // Convert DTO to domain request
        LoginRequest loginRequest = new LoginRequest(request.email(), request.password());

        // Authenticate
        var result = authenticationService.login(loginRequest);

        // Return response
        return result.fold(
            success -> Response.ok(LoginResponseDTO.from(success.token(), success.user())).build(),
            AuthErrorResponseMapper::toResponse
        );
    }
}
EOF
```

**Fix the typo in LoginRequestDTO:**

```bash
# Re-create LoginRequestDTO with fixed schema description
cat > src/main/java/com/k12/user/infrastructure/rest/dto/LoginRequestDTO.java << 'EOF'
package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Login request with email and password")
public record LoginRequestDTO(

    @Schema(description = "User email address", example = "admin@k12.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Schema(description = "User password", example = "admin123", required = true)
    @NotBlank(message = "Password is required")
    String password
) {}
EOF
```

**Step 6: Run test to verify it passes**

```bash
./mvnw test -Dtest=AuthResourceIntegrationTest
```
Expected: PASS (all 4 tests)

**Step 7: Verify endpoint with curl**

```bash
# Start Quarkus in dev mode (if not running)
./mvnw quarkus:dev &

# Wait for startup, then test
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@k12.com","password":"admin123"}' \
  | jq .
```
Expected: Returns JSON with token and user info

**Step 8: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/rest/ \
        src/test/java/com/k12/user/infrastructure/rest/
git commit -m "feat: add AuthResource REST endpoint

Add POST /api/auth/login endpoint for user authentication.
Returns JWT token on successful login, appropriate errors on failure.
Includes OpenAPI documentation and integration tests.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Verify token generation and claims

**Why:** Ensure JWT tokens are correctly formatted and contain all required claims.

**Step 1: Generate a test token**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@k12.com","password":"admin123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"
```

**Step 2: Decode and verify claims**

```bash
# Decode JWT payload (base64)
echo $TOKEN | cut -d. -f2 | base64 -d | jq .
```

Expected output should contain:
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@k12.com",
  "roles": "SUPER_ADMIN",
  "tenantId": "default-tenant",
  "iss": "k12-api",
  "iat": 1740277600,
  "exp": 1740364000
}
```

**Step 3: Verify token with public key**

```bash
# This verifies the token signature using the public key
# In production, SmallRye JWT does this automatically
echo "Token validation will be handled by SmallRye JWT on protected endpoints"
```

---

## Task 10: Update OpenAPI documentation

**Why:** Ensure API is properly documented.

**Step 1: Access OpenAPI UI**

```bash
# Open browser to
open http://localhost:8080/q/swagger-ui
# or
firefox http://localhost:8080/q/swagger-ui
```

**Step 2: Verify endpoint is documented**

Check that `/api/auth/login` appears with:
- Request body schema (LoginRequestDTO)
- Response schema (LoginResponseDTO for 200)
- Error response schema (ErrorResponseDTO for 401, 400)

**Step 3: Access OpenAPI spec**

```bash
curl http://localhost:8080/q/openapi | jq '.paths."/api/auth/login"'
```

---

## Final Verification

**Step 1: Run all tests**

```bash
./mvnw test
```
Expected: All tests pass

**Step 2: Check test coverage**

```bash
./mvnw clean test jacoco:report
cat target/jacoco-report/index.html | grep -A 5 "user/"
```

**Step 3: Manual testing checklist**

- [ ] Login with valid credentials → 200 + token
- [ ] Login with invalid password → 401
- [ ] Login with non-existent email → 401
- [ ] Login with invalid email format → 400
- [ ] Token contains all required claims
- [ ] Token is signed with RS256
- [ ] OpenAPI documentation is accessible

---

## Notes for Future Improvements

1. **Tenant Association**: Update User aggregate and users table to include tenant_id
2. **Refresh Tokens**: Consider adding refresh token support for better security
3. **Logout Endpoint**: Add token invalidation if needed
4. **Rate Limiting**: Add rate limiting to prevent brute force attacks
5. **Audit Logging**: Log all login attempts for security monitoring
6. **Password Reset**: Add forgot password flow
7. **MFA**: Add multi-factor authentication support

---

## Summary

This implementation plan creates a complete authentication system with:

- Event-sourced UserRepository following DDD patterns
- Domain service for credential validation
- JWT token generation with RS256
- REST endpoint for login
- Comprehensive test coverage
- OpenAPI documentation

All changes follow existing codebase patterns and use configured technologies.
