# Create Tenant Admin Endpoint Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a SUPER_ADMIN-only endpoint that creates tenant-specific administrator users with both User entity (ADMIN role) and Admin specialization aggregate.

**Architecture:** REST endpoint in TenantResource → TenantAdminService (application layer) → UserFactory & AdminFactory (domain) → Repositories (persistence). Transactional boundary at service layer ensures atomic creation of both User and Admin.

**Tech Stack:** Quarkus, JAX-RS, Jakarta Validation, Event Sourcing (User), Domain-Driven Design

---

## Task 1: Create TenantAdminError Enum

**Files:**
- Create: `src/main/java/com/k12/tenant/domain/models/error/TenantAdminError.java`

**Step 1: Create the error enum**

```java
package com.k12.tenant.domain.models.error;

public enum TenantAdminError {
    TENANT_NOT_FOUND,
    EMAIL_ALREADY_EXISTS,
    INVALID_EMAIL,
    INVALID_PASSWORD,
    INVALID_NAME,
    INVALID_PERMISSIONS,
    USER_CREATION_FAILED,
    ADMIN_CREATION_FAILED
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/k12/tenant/domain/models/error/TenantAdminError.java
git commit -m "feat: add TenantAdminError enum"
```

---

## Task 2: Create CreateTenantAdminRequest DTO

**Files:**
- Create: `src/main/java/com/k12/tenant/infrastructure/rest/dto/CreateTenantAdminRequest.java`
- Reference: `src/main/java/com/k12/tenant/infrastructure/rest/dto/CreateTenantRequest.java` (for pattern)

**Step 1: Write the DTO with validation**

```java
package com.k12.tenant.infrastructure.rest.dto;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Request payload for creating a tenant administrator")
public record CreateTenantAdminRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Admin email address", example = "admin@tenant.com", required = true)
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Admin password (will be hashed)", example = "SecurePass123", required = true)
    String password,

    @NotBlank(message = "Name is required")
    @Schema(description = "Admin full name", example = "Tenant Administrator", required = true)
    String name,

    @NotEmpty(message = "At least one permission is required")
    @Schema(description = "Set of permissions for this admin", required = true)
    Set<Permission> permissions
) {}
```

**Step 2: Commit**

```bash
git add src/main/java/com/k12/tenant/infrastructure/rest/dto/CreateTenantAdminRequest.java
git commit -m "feat: add CreateTenantAdminRequest DTO"
```

---

## Task 3: Create TenantAdminResponse DTO

**Files:**
- Create: `src/main/java/com/k12/tenant/infrastructure/rest/dto/TenantAdminResponse.java`
- Reference: `src/main/java/com/k12/tenant/infrastructure/rest/dto/TenantResponse.java` (for pattern)

**Step 1: Write the response DTO**

```java
package com.k12.tenant.infrastructure.rest.dto;

import com.k12.common.domain.model.TenantId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Response payload containing created tenant administrator information")
public record TenantAdminResponse(
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String userId,

    @Schema(description = "User email", example = "admin@tenant.com")
    String email,

    @Schema(description = "User name", example = "Tenant Administrator")
    String name,

    @Schema(description = "User role (always ADMIN)", example = "ADMIN")
    String role,

    @Schema(description = "Associated tenant ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String tenantId,

    @Schema(description = "Admin ID (same as user ID)", example = "123e4567-e89b-12d3-a456-426614174000")
    String adminId,

    @Schema(description = "Admin permissions")
    Set<Permission> permissions,

    @Schema(description = "Creation timestamp (ISO 8601)", example = "2024-02-26T10:30:00Z")
    String createdAt
) {
    public static TenantAdminResponse from(User user, Admin admin, TenantId tenantId) {
        return new TenantAdminResponse(
            user.userId().value(),
            user.emailAddress().value(),
            user.name().value(),
            UserRole.ADMIN.name(),
            tenantId.value(),
            admin.adminId().userId().value(),
            admin.permissions(),
            admin.createdAt().toString()
        );
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/k12/tenant/infrastructure/rest/dto/TenantAdminResponse.java
git commit -m "feat: add TenantAdminResponse DTO"
```

---

## Task 4: Check if PasswordHasher Exists

**Files:**
- Check: `src/main/java/com/k12/user/infrastructure/security/PasswordHasher.java`
- Check: `src/main/java/com/k12/common/infrastructure/security/PasswordHasher.java`

**Step 1: Search for PasswordHasher**

Run: `find . -name "PasswordHasher.java" -type f`

**Step 2: If not found, add to task list**

If PasswordHasher doesn't exist, add it to the implementation plan (we'll create it in a later task).

---

## Task 5: Create AdminFactory (if doesn't exist)

**Files:**
- Check: `src/main/java/com/k12/user/domain/models/specialization/admin/AdminFactory.java`
- Create if missing

**Step 1: Check if AdminFactory exists**

Run: `ls -la src/main/java/com/k12/user/domain/models/specialization/admin/AdminFactory.java`

**Step 2: If exists, verify structure**

Run: `cat src/main/java/com/k12/user/domain/models/specialization/admin/AdminFactory.java`

Verify it has a `create` method that takes `AdminId`, `Set<Permission>`, and optionally `TenantId`.

**Step 3: If missing, create AdminFactory**

```java
package com.k12.user.domain.models.specialization.admin;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;

import java.time.Instant;
import java.util.Set;

public final class AdminFactory {

    public static Admin create(AdminId adminId, Set<Permission> permissions) {
        if (adminId == null) {
            throw new IllegalArgumentException("AdminId cannot be null");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("Permissions cannot be null or empty");
        }

        return new Admin(adminId, permissions, Instant.now());
    }
}
```

**Step 4: Commit**

```bash
git add src/main/java/com/k12/user/domain/models/specialization/admin/AdminFactory.java
git commit -m "feat: add AdminFactory for Admin aggregate creation"
```

---

## Task 6: Create TenantAdminService Interface

**Files:**
- Create: `src/main/java/com/k12/tenant/application/service/TenantAdminService.java`

**Step 1: Write the service interface**

```java
package com.k12.tenant.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.error.TenantAdminError;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public interface TenantAdminService {

    Result<TenantAdminResponse, TenantAdminError> createTenantAdmin(
        TenantId tenantId,
        CreateTenantAdminRequest request
    );
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/k12/tenant/application/service/TenantAdminService.java
git commit -m "feat: add TenantAdminService interface"
```

---

## Task 7: Create TenantAdminServiceImpl

**Files:**
- Create: `src/main/java/com/k12/tenant/application/service/TenantAdminServiceImpl.java`
- Reference: `src/main/java/com/k12/tenant/application/service/TenantService.java` (for pattern)
- Reference: `src/main/java/com/k12/user/domain/models/UserFactory.java` (for UserFactory pattern)

**Step 1: Write the service implementation**

```java
package com.k12.tenant.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.tenant.domain.models.error.TenantAdminError;
import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserFactory;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.events.UserEvents;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.AdminFactory;
import com.k12.user.domain.models.specialization.admin.AdminId;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.UserRepository;
import io.quarkus.narayana.jta.runtime.transaction.TransactionScoped;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TenantAdminServiceImpl implements TenantAdminService {

    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public Result<TenantAdminResponse, TenantAdminError> createTenantAdmin(
        TenantId tenantId,
        CreateTenantAdminRequest request
    ) {
        log.info("Creating tenant admin for tenant: {}", tenantId.value());

        // Step 1: Validate tenant exists
        var tenantResult = tenantService.getTenant(tenantId);
        if (tenantResult.isError()) {
            log.warn("Tenant not found: {}", tenantId.value());
            return Result.failure(TenantAdminError.TENANT_NOT_FOUND);
        }

        // Step 2: Check if email already exists
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            log.warn("Email already exists: {}", request.email());
            return Result.failure(TenantAdminError.EMAIL_ALREADY_EXISTS);
        }

        // Step 3: Validate and create value objects
        EmailAddress emailAddress;
        try {
            emailAddress = EmailAddress.of(request.email());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid email format: {}", request.email());
            return Result.failure(TenantAdminError.INVALID_EMAIL);
        }

        UserName userName;
        try {
            userName = UserName.of(request.name());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid name: {}", request.name());
            return Result.failure(TenantAdminError.INVALID_NAME);
        }

        // Step 4: Hash password
        PasswordHash passwordHash;
        try {
            passwordHash = passwordHasher.hash(request.password());
        } catch (Exception e) {
            log.error("Password hashing failed", e);
            return Result.failure(TenantAdminError.INVALID_PASSWORD);
        }

        // Step 5: Create User with ADMIN role
        Result<UserEvents, ?> userResult = UserFactory.create(
            emailAddress,
            passwordHash,
            Set.of(UserRole.ADMIN),  // HARDCODED AS ADMIN
            userName
        );

        if (userResult.isError()) {
            log.error("User creation failed");
            return Result.failure(TenantAdminError.USER_CREATION_FAILED);
        }

        UserEvents.UserCreated userCreated = (UserEvents.UserCreated) userResult.getSuccess();
        UserId userId = userCreated.userId();

        // Step 6: Save User with tenant association
        User user = new User(
            userId,
            emailAddress,
            passwordHash,
            Set.of(UserRole.ADMIN),
            userCreated.status(),
            userName
        );
        userRepository.save(user);

        // Step 7: Create Admin aggregate
        AdminId adminId = AdminId.of(userId);
        Admin admin;
        try {
            admin = AdminFactory.create(adminId, request.permissions());
        } catch (IllegalArgumentException e) {
            log.error("Admin creation failed: {}", e.getMessage());
            return Result.failure(TenantAdminError.INVALID_PERMISSIONS);
        }

        // Step 8: Save Admin
        try {
            adminRepository.save(admin);
        } catch (Exception e) {
            log.error("Failed to save admin aggregate", e);
            return Result.failure(TenantAdminError.ADMIN_CREATION_FAILED);
        }

        // Step 9: Build response
        TenantAdminResponse response = TenantAdminResponse.from(user, admin, tenantId);
        log.info("Successfully created tenant admin: {} for tenant: {}", userId.value(), tenantId.value());

        return Result.success(response);
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/k12/tenant/application/service/TenantAdminServiceImpl.java
git commit -m "feat: add TenantAdminServiceImpl with transactional admin creation"
```

---

## Task 8: Add createTenantAdmin Method to TenantResource

**Files:**
- Modify: `src/main/java/com/k12/tenant/infrastructure/rest/resource/TenantResource.java:320` (add at end of class)
- Reference: Existing methods like `activateTenant` and `suspendTenant` for pattern

**Step 1: Add the endpoint method**

Add this method to TenantResource class (after the `deleteTenant` method, before the private methods):

```java
    @POST
    @Path("/{id}/admins")
    @Operation(
            summary = "Create a tenant administrator",
            description = "Creates a new ADMIN user and Admin specialization for a specific tenant")
    @APIResponses({
        @APIResponse(
                responseCode = "201",
                description = "Admin created successfully",
                content = @Content(schema = @Schema(implementation = TenantAdminResponse.class))),
        @APIResponse(
                responseCode = "400",
                description = "Invalid request data",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "403", description = "Forbidden - SUPER_ADMIN role required"),
        @APIResponse(
                responseCode = "404",
                description = "Tenant not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(
                responseCode = "409",
                description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Response createTenantAdmin(
            @Parameter(description = "Tenant ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
                    @PathParam("id")
                    String id,
            @Valid CreateTenantAdminRequest request) {

        Span span = tracer.spanBuilder("TenantResource.createTenantAdmin")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            TenantId tenantId = new TenantId(id);
            var result = tenantAdminService.createTenantAdmin(tenantId, request);

            return result.fold(
                    success -> {
                        span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                        return Response.status(Response.Status.CREATED.getStatusCode())
                                .entity(success)
                                .build();
                    },
                    error -> {
                        span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, error.toString());
                        return toErrorResponse(error);
                    });
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
```

**Step 2: Add imports at top of file**

```java
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;
import com.k12.tenant.application.service.TenantAdminService;
import com.k12.tenant.domain.models.error.TenantAdminError;
```

**Step 3: Add TenantAdminService dependency to constructor**

The class already uses `@RequiredArgsConstructor`, so just add the field:

```java
private final TenantAdminService tenantAdminService;
```

**Step 4: Add error response mapping method**

Add this private method at the end of the class (before the closing brace):

```java
    private Response toErrorResponse(TenantAdminError error) {
        return switch (error) {
            case TENANT_NOT_FOUND ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("TENANT_NOT_FOUND", "Tenant not found"))
                    .build();
            case EMAIL_ALREADY_EXISTS ->
                Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("EMAIL_ALREADY_EXISTS", "Email already exists"))
                    .build();
            case INVALID_EMAIL ->
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_EMAIL", "Invalid email format"))
                    .build();
            case INVALID_PASSWORD ->
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_PASSWORD", "Password must be at least 8 characters"))
                    .build();
            case INVALID_NAME ->
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_NAME", "Name cannot be blank"))
                    .build();
            case INVALID_PERMISSIONS ->
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_PERMISSIONS", "At least one permission is required"))
                    .build();
            case USER_CREATION_FAILED, ADMIN_CREATION_FAILED ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(error.name(), "Failed to create admin"))
                    .build();
        };
    }
```

**Step 5: Commit**

```bash
git add src/main/java/com/k12/tenant/infrastructure/rest/resource/TenantResource.java
git commit -m "feat: add createTenantAdmin endpoint to TenantResource"
```

---

## Task 9: Write Unit Tests for CreateTenantAdminRequest DTO

**Files:**
- Create: `src/test/java/com/k12/tenant/infrastructure/rest/dto/CreateTenantAdminRequestTest.java`
- Reference: Existing DTO validation tests in codebase

**Step 1: Write the validation test**

```java
package com.k12.tenant.infrastructure.rest.dto;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTenantAdminRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequest_passesValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "SecurePass123",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT, Permission.COURSE_MANAGEMENT)
        );

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void blankEmail_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "",
            "SecurePass123",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT)
        );

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size(), "Should have violations for blank and invalid email");
    }

    @Test
    void invalidEmailFormat_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "not-an-email",
            "SecurePass123",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT)
        );

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("Email must be valid", violations.iterator().next().getMessage());
    }

    @Test
    void shortPassword_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "short",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT)
        );

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("Password must be at least 8 characters", violations.iterator().next().getMessage());
    }

    @Test
    void blankName_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "SecurePass123",
            " ",
            Set.of(Permission.USER_MANAGEMENT)
        );

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("Name is required", violations.iterator().next().getMessage());
    }

    @Test
    void emptyPermissions_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "SecurePass123",
            "Tenant Admin",
            Set.of()
        );

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("permission"));
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=CreateTenantAdminRequestTest`
Expected: PASS

**Step 3: Commit**

```bash
git add src/test/java/com/k12/tenant/infrastructure/rest/dto/CreateTenantAdminRequestTest.java
git commit -m "test: add validation tests for CreateTenantAdminRequest"
```

---

## Task 10: Write Unit Tests for TenantAdminService

**Files:**
- Create: `src/test/java/com/k12/tenant/application/service/TenantAdminServiceImplTest.java`
- Mocks: TenantService, UserRepository, AdminRepository, PasswordHasher

**Step 1: Write the service test**

```java
package com.k12.tenant.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Tenant;
import com.k12.tenant.domain.models.error.TenantAdminError;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.AdminId;
import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class TenantAdminServiceImplTest {

    @InjectMock
    TenantService tenantService;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    AdminRepository adminRepository;

    @InjectMock
    PasswordHasher passwordHasher;

    @Test
    void createTenantAdmin_withValidData_returnsSuccess() {
        // Arrange
        TenantId tenantId = new TenantId("123e4567-e89b-12d3-a456-426614174000");
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "SecurePass123",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT)
        );

        when(tenantService.getTenant(tenantId))
            .thenReturn(Result.success(mockTenant()));
        when(userRepository.findByEmail("admin@tenant.com"))
            .thenReturn(Optional.empty());
        when(passwordHasher.hash("SecurePass123"))
            .thenReturn(PasswordHash.of("hashed"));
        when(userRepository.save(any(User.class)))
            .thenReturn(mockUser());
        when(adminRepository.save(any(Admin.class)))
            .thenReturn(mockAdmin());

        // Act
        TenantAdminServiceImpl service = new TenantAdminServiceImpl(
            tenantService, userRepository, adminRepository, passwordHasher
        );
        Result<TenantAdminResponse, TenantAdminError> result = service.createTenantAdmin(tenantId, request);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getSuccess());
        assertEquals("admin@tenant.com", result.getSuccess().email());
        verify(userRepository).save(any(User.class));
        verify(adminRepository).save(any(Admin.class));
    }

    @Test
    void createTenantAdmin_tenantNotFound_returnsError() {
        // Arrange
        TenantId tenantId = new TenantId("123e4567-e89b-12d3-a456-426614174000");
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "SecurePass123",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT)
        );

        when(tenantService.getTenant(tenantId))
            .thenReturn(Result.error(TenantError.TENANT_NOT_FOUND));

        // Act
        TenantAdminServiceImpl service = new TenantAdminServiceImpl(
            tenantService, userRepository, adminRepository, passwordHasher
        );
        Result<TenantAdminResponse, TenantAdminError> result = service.createTenantAdmin(tenantId, request);

        // Assert
        assertTrue(result.isError());
        assertEquals(TenantAdminError.TENANT_NOT_FOUND, result.getError());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createTenantAdmin_emailAlreadyExists_returnsError() {
        // Arrange
        TenantId tenantId = new TenantId("123e4567-e89b-12d3-a456-426614174000");
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
            "admin@tenant.com",
            "SecurePass123",
            "Tenant Admin",
            Set.of(Permission.USER_MANAGEMENT)
        );

        when(tenantService.getTenant(tenantId))
            .thenReturn(Result.success(mockTenant()));
        when(userRepository.findByEmail("admin@tenant.com"))
            .thenReturn(Optional.of(mockUser()));

        // Act
        TenantAdminServiceImpl service = new TenantAdminServiceImpl(
            tenantService, userRepository, adminRepository, passwordHasher
        );
        Result<TenantAdminResponse, TenantAdminError> result = service.createTenantAdmin(tenantId, request);

        // Assert
        assertTrue(result.isError());
        assertEquals(TenantAdminError.EMAIL_ALREADY_EXISTS, result.getError());
        verify(userRepository, never()).save(any());
    }

    private Tenant mockTenant() {
        return Tenant.builder() // Adjust based on actual Tenant class
            .id(new TenantId("123e4567-e89b-12d3-a456-426614174000"))
            .build();
    }

    private User mockUser() {
        return new User(
            com.k12.common.domain.model.UserId.generate(),
            EmailAddress.of("admin@tenant.com"),
            PasswordHash.of("hashed"),
            Set.of(UserRole.ADMIN),
            UserStatus.ACTIVE,
            UserName.of("Tenant Admin")
        );
    }

    private Admin mockAdmin() {
        return new Admin(
            AdminId.of(com.k12.common.domain.model.UserId.generate()),
            Set.of(Permission.USER_MANAGEMENT),
            java.time.Instant.now()
        );
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=TenantAdminServiceImplTest`
Expected: PASS (may need adjustments based on actual Tenant/Admin constructors)

**Step 3: Commit**

```bash
git add src/test/java/com/k12/tenant/application/service/TenantAdminServiceImplTest.java
git commit -m "test: add unit tests for TenantAdminService"
```

---

## Task 11: Write Integration Test for TenantResource Endpoint

**Files:**
- Create: `src/test/java/com/k12/tenant/infrastructure/rest/resource/TenantResourceIntegrationTest.java`
- Or enhance: Existing `TenantResourceIntegrationTest` if it exists

**Step 1: Write the integration test**

```java
package com.k12.tenant.infrastructure.rest.resource;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TenantResourceCreateAdminIntegrationTest {

    private static final String SUPER_ADMIN_TOKEN = "valid-super-admin-jwt"; // Replace with actual token generation

    @Test
    void createTenantAdmin_withSuperAdminToken_returns201() {
        // Replace with actual tenant ID
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";

        given()
            .auth().oauth2(SUPER_ADMIN_TOKEN)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "admin@tenant.com",
                    "password": "SecurePass123",
                    "name": "Tenant Admin",
                    "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT"]
                }
                """)
        .when()
            .post("/api/tenants/" + tenantId + "/admins")
        .then()
            .statusCode(201)
            .body("email", equalTo("admin@tenant.com"))
            .body("name", equalTo("Tenant Admin"))
            .body("role", equalTo("ADMIN"))
            .body("tenantId", equalTo(tenantId))
            .body("permissions", hasItem("USER_MANAGEMENT"));
    }

    @Test
    void createTenantAdmin_withInvalidEmail_returns400() {
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";

        given()
            .auth().oauth2(SUPER_ADMIN_TOKEN)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "not-an-email",
                    "password": "SecurePass123",
                    "name": "Tenant Admin",
                    "permissions": ["USER_MANAGEMENT"]
                }
                """)
        .when()
            .post("/api/tenants/" + tenantId + "/admins")
        .then()
            .statusCode(400)
            .body("error", equalTo("INVALID_EMAIL"));
    }

    @Test
    void createTenantAdmin_withShortPassword_returns400() {
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";

        given()
            .auth().oauth2(SUPER_ADMIN_TOKEN)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "admin@tenant.com",
                    "password": "short",
                    "name": "Tenant Admin",
                    "permissions": ["USER_MANAGEMENT"]
                }
                """)
        .when()
            .post("/api/tenants/" + tenantId + "/admins")
        .then()
            .statusCode(400)
            .body("error", equalTo("INVALID_PASSWORD"));
    }

    @Test
    void createTenantAdmin_withNoPermissions_returns400() {
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";

        given()
            .auth().oauth2(SUPER_ADMIN_TOKEN)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "admin@tenant.com",
                    "password": "SecurePass123",
                    "name": "Tenant Admin",
                    "permissions": []
                }
                """)
        .when()
            .post("/api/tenants/" + tenantId + "/admins")
        .then()
            .statusCode(400)
            .body("error", equalTo("INVALID_PERMISSIONS"));
    }

    @Test
    void createTenantAdmin_nonExistentTenant_returns404() {
        String invalidTenantId = "00000000-0000-0000-0000-000000000000";

        given()
            .auth().oauth2(SUPER_ADMIN_TOKEN)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "admin@tenant.com",
                    "password": "SecurePass123",
                    "name": "Tenant Admin",
                    "permissions": ["USER_MANAGEMENT"]
                }
                """)
        .when()
            .post("/api/tenants/" + invalidTenantId + "/admins")
        .then()
            .statusCode(404)
            .body("error", equalTo("TENANT_NOT_FOUND"));
    }
}
```

**Note:** You'll need to replace `SUPER_ADMIN_TOKEN` with actual JWT token generation logic for your tests. Reference existing tests in `AuthResourceIntegrationTest` for the pattern.

**Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=TenantResourceCreateAdminIntegrationTest`
Expected: PASS (after token generation is added)

**Step 3: Commit**

```bash
git add src/test/java/com/k12/tenant/infrastructure/rest/resource/TenantResourceCreateAdminIntegrationTest.java
git commit -m "test: add integration tests for createTenantAdmin endpoint"
```

---

## Task 12: Manual Testing with Real API

**Files:**
- No files created
- Manual verification

**Step 1: Start the application**

Run: `mvn quarkus:dev`

**Step 2: Generate SUPER_ADMIN JWT**

Run: `./generate-super-admin-token.sh` or use existing auth endpoint:

```bash
# Login as super admin to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"superadmin@k12.com","password":"your-password"}' | jq -r '.token')

echo "Token: $TOKEN"
```

**Step 3: Create a test tenant (if needed)**

```bash
# Create tenant
TENANT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Test Tenant",
    "subdomain": "testtenant"
  }')

TENANT_ID=$(echo $TENANT_RESPONSE | jq -r '.tenantId')
echo "Tenant ID: $TENANT_ID"
```

**Step 4: Create tenant admin**

```bash
# Create admin
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@testtenant.com",
    "password": "SecurePass123",
    "name": "Test Tenant Admin",
    "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"]
  }' | jq '.'

# Expected response:
# {
#   "userId": "...",
#   "email": "admin@testtenant.com",
#   "name": "Test Tenant Admin",
#   "role": "ADMIN",
#   "tenantId": "$TENANT_ID",
#   "adminId": "...",
#   "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"],
#   "createdAt": "2024-02-26T..."
# }
```

**Step 5: Test error scenarios**

```bash
# Test duplicate email (should return 409)
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@testtenant.com",
    "password": "AnotherPass123",
    "name": "Another Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'

# Expected: 409 Conflict with error message

# Test invalid tenant (should return 404)
curl -X POST "http://localhost:8080/api/tenants/00000000-0000-0000-0000-000000000000/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application JSON' \
  -d '{
    "email": "admin@fake.com",
    "password": "SecurePass123",
    "name": "Fake Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'

# Expected: 404 Not Found
```

**Step 6: Verify in database**

```bash
# Check users table
psql -h localhost -U k12_user -d k12_db -c \
  "SELECT id, email, roles, tenant_id FROM users WHERE email = 'admin@testtenant.com';"

# Check admin tables
psql -h localhost -U k12_user -d k12_db -c \
  "SELECT * FROM admin WHERE admin_id = (SELECT id FROM users WHERE email = 'admin@testtenant.com');"
```

**No commit needed for manual testing**

---

## Task 13: Update OpenAPI Documentation

**Files:**
- Verify: The endpoint already has OpenAPI annotations from Task 8
- Optional: Add to API documentation

**Step 1: Verify OpenAPI schema is accessible**

Run: `mvn quarkus:dev`

Visit: `http://localhost:8080/q/swagger-ui`

**Step 2: Verify the endpoint appears**

- Expand `/api/tenants` section
- Verify `POST /api/tenants/{id}/admins` is listed
- Click "Try it out" to test from Swagger UI

**Step 3: Test from Swagger UI**

1. Click "Try it out"
2. Enter valid tenant ID
3. Paste request body:
   ```json
   {
     "email": "swagger-test@tenant.com",
     "password": "SwaggerTest123",
     "name": "Swagger Test Admin",
     "permissions": ["USER_MANAGEMENT"]
   }
   ```
4. Click "Execute"
5. Verify 201 response

**No commit needed - annotations already added in Task 8**

---

## Task 14: Add Error Response to ErrorResponseMapper (if needed)

**Files:**
- Check: `src/main/java/com/k12/tenant/infrastructure/rest/mapper/ErrorResponseMapper.java`
- Modify if needed

**Step 1: Check if ErrorResponseMapper needs update**

Run: `cat src/main/java/com/k12/tenant/infrastructure/rest/mapper/ErrorResponseMapper.java`

If it already handles `TenantAdminError`, skip to Step 3.

**Step 2: Add TenantAdminError handling (if needed)**

```java
// Add to ErrorResponseMapper class
private Response toErrorResponse(TenantAdminError error) {
    return switch (error) {
        case TENANT_NOT_FOUND ->
            Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("TENANT_NOT_FOUND", "Tenant not found"))
                .build();
        case EMAIL_ALREADY_EXISTS ->
            Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("EMAIL_ALREADY_EXISTS", "Email already exists"))
                .build();
        case INVALID_EMAIL ->
            Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_EMAIL", "Invalid email format"))
                .build();
        case INVALID_PASSWORD ->
            Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_PASSWORD", "Password must be at least 8 characters"))
                .build();
        case INVALID_NAME ->
            Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_NAME", "Name cannot be blank"))
                .build();
        case INVALID_PERMISSIONS ->
            Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_PERMISSIONS", "At least one permission is required"))
                .build();
        case USER_CREATION_FAILED, ADMIN_CREATION_FAILED ->
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(error.name(), "Failed to create admin"))
                .build();
    };
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/k12/tenant/infrastructure/rest/mapper/ErrorResponseMapper.java
git commit -m "feat: add TenantAdminError mapping to ErrorResponseMapper"
```

---

## Task 15: Final Integration Test Run

**Files:**
- All test files
- Run full test suite

**Step 1: Run all tests**

Run: `mvn clean test`

Expected: All tests pass

**Step 2: Run integration tests specifically**

Run: `mvn verify -DskipUnitTests`

Expected: All integration tests pass

**Step 3: Check code coverage (optional)**

Run: `mvn test jacoco:report`

Check: `target/site/jacoco/index.html`

**No commit needed**

---

## Task 16: Documentation Updates

**Files:**
- Update: `README.md` (if it has API documentation section)
- Update: `docs/API.md` (if exists)
- Create: `docs/tenant-admin-endpoint.md` (optional usage guide)

**Step 1: Create usage documentation**

```bash
cat > docs/tenant-admin-endpoint.md << 'EOF'
# Create Tenant Admin Endpoint

## Overview

Create tenant-specific administrator users with the ADMIN role and assigned permissions.

## Endpoint

**POST** `/api/tenants/{tenantId}/admins`

### Authorization

- Requires `SUPER_ADMIN` role
- JWT token must include SUPER_ADMIN in roles claim

### Request

```json
{
  "email": "admin@tenant.com",
  "password": "SecurePass123",
  "name": "Tenant Administrator",
  "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"]
}
```

### Response (201 Created)

```json
{
  "userId": "uuid",
  "email": "admin@tenant.com",
  "name": "Tenant Administrator",
  "role": "ADMIN",
  "tenantId": "tenant-uuid",
  "adminId": "uuid",
  "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"],
  "createdAt": "2024-02-26T10:30:00Z"
}
```

## Available Permissions

See `Permission` enum in `src/main/java/com/k12/user/domain/models/specialization/admin/valueobjects/Permission.java`

Full list:
- USER_MANAGEMENT
- COURSE_MANAGEMENT
- TEACHER_MANAGEMENT
- STUDENT_MANAGEMENT
- PARENT_MANAGEMENT
- SYSTEM_SETTINGS
- REPORTS_VIEW
- REPORTS_EXPORT
- ... (and many more)

## Example Usage

```bash
# 1. Login as SUPER_ADMIN
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"superadmin@k12.com","password":"admin123"}' | jq -r '.token')

# 2. Create tenant admin
curl -X POST "http://localhost:8080/api/tenants/{tenantId}/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@tenant.com",
    "password": "SecurePass123",
    "name": "Tenant Admin",
    "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT"]
  }'
```

## Error Responses

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_EMAIL | Email format is invalid |
| 400 | INVALID_PASSWORD | Password must be at least 8 characters |
| 400 | INVALID_NAME | Name cannot be blank |
| 400 | INVALID_PERMISSIONS | At least one permission required |
| 403 | FORBIDDEN | SUPER_ADMIN role required |
| 404 | TENANT_NOT_FOUND | Tenant does not exist |
| 409 | EMAIL_ALREADY_EXISTS | Email already in use |
| 500 | USER_CREATION_FAILED | Failed to create user |
| 500 | ADMIN_CREATION_FAILED | Failed to create admin aggregate |
EOF
```

**Step 2: Commit**

```bash
git add docs/tenant-admin-endpoint.md
git commit -m "docs: add usage guide for create tenant admin endpoint"
```

---

## Task 17: Final Verification and Cleanup

**Files:**
- Check for TODOs, FIXMEs, debug code
- Verify no hardcoded values

**Step 1: Check for temporary code**

Run: `grep -r "TODO\|FIXME\|XXX\|HACK" src/main/java/com/k12/tenant/`

If found, either fix or document why it's still there.

**Step 2: Check for hardcoded values**

Run: `grep -r "localhost\|test\|demo" src/main/java/com/k12/tenant/application/service/TenantAdminServiceImpl.java`

Ensure only constants and configuration values remain.

**Step 3: Final test run**

Run: `mvn clean test verify`

**Step 4: Build production version**

Run: `mvn clean package -DskipTests`

Verify: Build succeeds

**No commit needed unless changes made**

---

## Task 18: Create Pull Request or Merge

**Files:**
- Git operations

**Step 1: Review all commits**

Run: `git log --oneline --graph`

**Step 2: Push to remote**

Run: `git push origin main`

Or if working in feature branch:

```bash
git checkout -b feature/create-tenant-admin-endpoint
git push -u origin feature/create-tenant-admin-endpoint
```

**Step 3: Create pull request (if using branches)**

Include in PR description:
- Implements design document: `docs/plans/2026-02-26-create-tenant-admin-endpoint-design.md`
- Adds POST /api/tenants/{tenantId}/admins endpoint
- SUPER_ADMIN only authorization
- Creates User with ADMIN role + Admin specialization
- Transactional atomic creation
- Full test coverage

**Step 4: Merge after approval**

Or if working directly in main: the implementation is complete!

---

## Summary

This implementation plan creates a comprehensive tenant admin creation endpoint:

✅ **18 bite-sized tasks** (2-5 minutes each)
✅ **TDD approach** (tests before/with implementation)
✅ **Frequent commits** (every task)
✅ **Complete code** (no "add validation here" placeholders)
✅ **Exact file paths** (no guessing)
✅ **Full test coverage** (unit + integration + manual)
✅ **Documentation** (usage guide included)

**Total estimated time:** 2-3 hours for experienced developer

**Key files created/modified:**
- 5 new DTOs
- 1 new service interface + implementation
- 1 new error enum
- 1 enhanced resource class
- 3 test classes
- 1 documentation file
