# Create Tenant Admin Endpoint Design

**Date:** 2026-02-26
**Author:** Design Document
**Status: Approved

## Overview

Create a new REST endpoint that allows SUPER_ADMIN users to create tenant-specific administrator accounts. The endpoint creates both a User entity with the ADMIN role and an Admin specialization aggregate, associating them with a specific tenant.

## Problem Statement

Currently, there is no way to create tenant-specific administrator users through the API. SUPER_ADMIN users need the ability to:
1. Create admin accounts for specific tenants
2. Assign specific permissions to each admin
3. Ensure proper tenant isolation and authorization

## Goals

1. **Authorization:** Restrict endpoint to SUPER_ADMIN role only
2. **Validation:** Ensure only ADMIN role can be created (error otherwise)
3. **Tenant Association:** Associate the created user with a specific tenant
4. **Dual Creation:** Create both User entity and Admin specialization atomically
5. **Permission Management:** Allow explicit permission assignment during creation

## Requirements

### Functional Requirements

- [ ] Endpoint accessible only to SUPER_ADMIN role
- [ ] Validate that role being created is ADMIN (error if not)
- [ ] Create User with ADMIN role associated to tenant_id
- [ ] Create Admin specialization aggregate with permissions
- [ ] Atomic transaction: rollback user creation if admin creation fails
- [ ] Validate tenant exists before creating admin
- [ ] Validate email uniqueness across system
- [ ] Hash password before storage

### Non-Functional Requirements

- Response time: < 500ms for successful creation
- Transactional integrity: User + Admin created atomically
- Security: Password hashing, authorization checks
- Validation: Comprehensive input validation

## Architecture

### Endpoint Design

**Path:** `POST /api/tenants/{tenantId}/admins`
**Authorization:** `@RolesAllowed("SUPER_ADMIN")`
**Content-Type:** `application/json`

### Request/Response Structure

**Request:**
```json
{
  "email": "admin@tenant.com",
  "password": "securePassword123",
  "name": "Tenant Administrator",
  "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"]
}
```

**Response (201 Created):**
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

### Component Diagram

```
POST /api/tenants/{tenantId}/admins
    │
    ▼
TenantResource.createTenantAdmin()
    │
    ▼
TenantAdminService.createTenantAdmin()
    │
    ├─► TenantService.getTenant(tenantId) - Validate tenant exists
    │
    ├─► Request validation (email, password, name, permissions)
    │
    ├─► PasswordHasher.hash(password)
    │
    ├─► UserFactory.create(email, hash, Set.of(ADMIN), name)
    │      └─► Returns UserCreated event
    │
    ├─► UserRepository.save(user) - Set tenant_id
    │
    ├─► AdminFactory.create(adminId, permissions, tenantId)
    │
    └─► AdminRepository.save(admin)
```

## Components

### 1. CreateTenantAdminRequest DTO (NEW)

**Package:** `com.k12.tenant.infrastructure.rest.dto`

```java
public record CreateTenantAdminRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Name is required")
    String name,

    @NotEmpty(message = "At least one permission is required")
    Set<Permission> permissions
) {}
```

### 2. TenantAdminResponse DTO (NEW)

**Package:** `com.k12.tenant.infrastructure.rest.dto`

```java
public record TenantAdminResponse(
    String userId,
    String email,
    String name,
    String role,
    String tenantId,
    String adminId,
    Set<Permission> permissions,
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

### 3. TenantAdminService (NEW)

**Package:** `com.k12.tenant.application.service`

```java
@ApplicationScoped
public class TenantAdminService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TenantService tenantService;
    private final PasswordHasher passwordHasher;

    @Transactional
    public Result<TenantAdminResponse, TenantAdminError> createTenantAdmin(
        TenantId tenantId,
        CreateTenantAdminRequest request
    ) {
        // Implementation in detailed plan
    }
}
```

### 4. AdminFactory (NEW or ENHANCE)

**Package:** `com.k12.user.domain.models.specialization.admin`

```java
public final class AdminFactory {

    public static Result<Admin, AdminError> create(
        AdminId adminId,
        Set<Permission> permissions,
        TenantId tenantId
    ) {
        if (permissions == null || permissions.isEmpty()) {
            return Result.failure(AdminError.PERMISSIONS_REQUIRED);
        }

        return Result.success(new Admin(
            adminId,
            permissions,
            Instant.now()
        ));
    }
}
```

### 5. TenantResource Enhancement

**Package:** `com.k12.tenant.infrastructure.rest.resource`

Add method to existing TenantResource class:

```java
@POST
@Path("/{id}/admins")
@Operation(
    summary = "Create a tenant administrator",
    description = "Creates a new ADMIN user and Admin specialization for a specific tenant"
)
@APIResponses({
    @APIResponse(responseCode = "201", description = "Admin created successfully"),
    @APIResponse(responseCode = "400", description = "Invalid request data"),
    @APIResponse(responseCode = "403", description = "Forbidden - SUPER_ADMIN role required"),
    @APIResponse(responseCode = "404", description = "Tenant not found"),
    @APIResponse(responseCode = "409", description = "Email already exists")
})
public Response createTenantAdmin(
    @PathParam("id") String id,
    @Valid CreateTenantAdminRequest request
) {
    // Implementation in detailed plan
}
```

## Data Flow

### Request Processing Sequence

1. **Authentication & Authorization**
   - JWTAuthenticationFilter validates JWT token
   - Checks `SUPER_ADMIN` role in token
   - Returns 403 if not SUPER_ADMIN

2. **Request Extraction**
   - Extract `tenantId` from `@PathParam("id")`
   - Validate `tenantId` format (UUID)

3. **Service Layer Processing**
   - `TenantAdminService.createTenantAdmin()` called
   - `@Transactional` ensures atomic operation

4. **Tenant Validation**
   - `TenantService.getTenant(tenantId)` called
   - Returns `TENANT_NOT_FOUND` error if tenant doesn't exist

5. **Request Validation**
   - Email format validation
   - Password strength validation
   - Name validation (not blank)
   - Permissions validation (not empty, valid enum values)

6. **Password Hashing**
   - `PasswordHasher.hash(request.password())`
   - Returns `PasswordHash` value object

7. **User Creation**
   - `UserFactory.create(email, hash, Set.of(ADMIN), name)`
   - **Role is hardcoded as ADMIN** - cannot be overridden
   - Returns `UserCreated` event

8. **User Persistence**
   - `UserRepository.save(user)` with `tenantId`
   - Database sets `tenant_id` column in users table

9. **Admin Aggregate Creation**
   - Extract `userId` from UserCreated event
   - Create `AdminId` wrapping `userId`
   - `AdminFactory.create(adminId, permissions, tenantId)`
   - Returns `Admin` aggregate

10. **Admin Persistence**
    - `AdminRepository.save(admin)`
    - Persists Admin aggregate with tenant association

11. **Response Building**
    - Build `TenantAdminResponse` from User, Admin, TenantId
    - Return 201 Created with response body

### Transaction Boundaries

The entire operation is wrapped in `@Transactional`:
- If Admin creation fails → User creation rolled back
- If Repository save fails → Entire operation rolled back
- Ensures no orphaned Users without Admin aggregates

## Error Handling

### Error Response Structure

All errors follow existing `ErrorResponse` pattern:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "details": { ... }
}
```

### Error Scenarios

| Scenario | HTTP Status | Error Code | Message |
|----------|-------------|------------|---------|
| Not authenticated | 401 | UNAUTHORIZED | "Authentication required" |
| Not SUPER_ADMIN | 403 | FORBIDDEN | "SUPER_ADMIN role required" |
| Tenant not found | 404 | TENANT_NOT_FOUND | "Tenant {id} not found" |
| Invalid UUID format | 400 | INVALID_TENANT_ID | "Tenant ID must be valid UUID" |
| Invalid email format | 400 | INVALID_EMAIL | "Email format is invalid" |
| Password too short | 400 | INVALID_PASSWORD | "Password must be at least 8 characters" |
| Name is blank | 400 | INVALID_NAME | "Name cannot be blank" |
| No permissions | 400 | INVALID_PERMISSIONS | "At least one permission required" |
| Invalid permission value | 400 | INVALID_PERMISSION | "Permission {x} is not valid" |
| Email already exists | 409 | EMAIL_ALREADY_EXISTS | "User with email {x} already exists" |
| User creation fails | 500 | USER_CREATION_FAILED | "Failed to create user" |
| Admin creation fails | 500 | ADMIN_CREATION_FAILED | "Failed to create admin aggregate" |

### Role Validation

The endpoint enforces ADMIN role through:
1. **Hardcoded role in UserFactory call**: `Set.of(UserRole.ADMIN)`
2. **No role parameter in request DTO** - prevents other roles from being passed
3. **Service-level validation**: Explicit check before UserFactory call
4. **Authorization check**: `@RolesAllowed("SUPER_ADMIN")` ensures only privileged users can create admins

## Validation Rules

### Input Validation

| Field | Type | Required | Validation | Default |
|-------|------|----------|------------|---------|
| tenantId | UUID | Yes | Valid UUID, exists in DB | - |
| email | String | Yes | Valid email format, unique | - |
| password | String | Yes | Min 8 characters | - |
| name | String | Yes | Not blank | - |
| permissions | Set<Permission> | Yes | Not empty, valid enum values | - |
| role | UserRole | No | Fixed as ADMIN | ADMIN |

### Business Logic Validation

1. **Tenant Existence**: Verify tenant exists before creating admin
2. **Email Uniqueness**: Check email doesn't exist in users table
3. **Permission Values**: All permissions must be valid Permission enum values
4. **Transaction Integrity**: Both User and Admin must be created successfully

## Testing Strategy

### Unit Tests

#### 1. TenantAdminServiceTest
- ✅ Successful admin creation with valid data
- ✅ Returns TENANT_NOT_FOUND when tenant doesn't exist
- ✅ Returns EMAIL_ALREADY_EXISTS when email in use
- ✅ Returns INVALID_EMAIL for malformed email
- ✅ Returns INVALID_PASSWORD for short password
- ✅ Returns INVALID_NAME for blank name
- ✅ Returns INVALID_PERMISSIONS for empty permission set
- ✅ Transaction rollback when admin save fails
- ✅ Transaction rollback when user save fails

#### 2. CreateTenantAdminRequestTest
- ✅ Valid request passes all validations
- ✅ Blank email fails validation
- ✅ Invalid email format fails validation
- ✅ Short password fails validation
- ✅ Blank name fails validation
- ✅ Empty permissions fail validation
- ✅ Invalid permission enum value fails validation

#### 3. AdminFactoryTest (if new)
- ✅ Creates Admin with valid permissions
- ✅ Returns error for null permissions
- ✅ Returns error for empty permissions
- ✅ Sets createdAt timestamp correctly

#### 4. TenantAdminResponseTest
- ✅ Correctly builds from User, Admin, TenantId
- ✅ All fields mapped correctly
- ✅ Role is always "ADMIN"

### Integration Tests

#### 1. TenantResourceIntegrationTest
```java
@Test
void createTenantAdmin_withSuperAdminToken_returns201() {
    // Generate SUPER_ADMIN JWT
    // POST to /api/tenants/{tenantId}/admins
    // Verify 201 status
    // Verify response body structure
    // Verify user created in DB with tenant_id
    // Verify admin created in DB
}

@Test
void createTenantAdmin_withAdminToken_returns403() {
    // Generate ADMIN JWT (not SUPER_ADMIN)
    // POST to /api/tenants/{tenantId}/admins
    // Verify 403 Forbidden
}

@Test
void createTenantAdmin_nonExistentTenant_returns404() {
    // Use invalid tenantId
    // Verify 404 Not Found
}

@Test
void createTenantAdmin_duplicateEmail_returns409() {
    // Create user with email
    // Try to create admin with same email
    // Verify 409 Conflict
}
```

#### 2. Database Integration Tests
- Verify `tenant_id` correctly set in users table
- Verify admin tables correctly populated
- Verify foreign key constraints
- Test transaction rollback scenarios

### Manual Testing

```bash
# 1. Generate SUPER_ADMIN token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"superadmin@k12.com","password":"admin123"}' | jq -r '.token')

# 2. Create tenant admin
curl -X POST http://localhost:8080/api/tenants/{tenantId}/admins \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@tenant.com",
    "password": "SecurePass123",
    "name": "Tenant Admin",
    "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT"]
  }'

# 3. Verify response (should be 201)
# 4. Try with non-SUPER_ADMIN token (should be 403)
# 5. Try with invalid tenantId (should be 404)
# 6. Try with duplicate email (should be 409)
```

## Security Considerations

### Authorization
- Endpoint protected with `@RolesAllowed("SUPER_ADMIN")`
- JWT filter validates token before endpoint execution
- Only users with SUPER_ADMIN role can access

### Password Handling
- Password never logged or returned in responses
- Password hashed using `PasswordHasher` before storage
- Hash uses secure algorithm (bcrypt/argon2)

### Tenant Isolation
- User's `tenant_id` column ensures tenant association
- Query filtering by tenant_id in repositories
- Admin can only access their tenant's resources

### Input Validation
- All inputs validated at multiple layers
- Email format validated
- Password strength enforced
- Permission values validated against enum

## Database Impact

### Tables Affected

1. **users** (INSERT)
   - `id`: User UUID
   - `email`: User email (unique constraint)
   - `password_hash`: Hashed password
   - `roles`: "ADMIN"
   - `status`: "ACTIVE"
   - `name`: User name
   - `tenant_id`: Tenant UUID (foreign key)
   - `version`, `created_at`, `updated_at`

2. **admin** or equivalent (INSERT)
   - `admin_id`: Same as user_id
   - `permissions`: JSON array of permissions
   - `tenant_id`: Tenant UUID
   - `created_at`: Timestamp

3. **user_events** (INSERT for event sourcing)
   - Event: `UserCreated`
   - Stored in event store

### Constraints
- Foreign key: `users.tenant_id → tenants.id` (ON DELETE SET NULL)
- Unique: `users.email`
- Not null: All required fields

## Open Questions

1. **Admin Persistence Schema**: Exact structure of admin persistence layer needs clarification (tables, columns)
2. **Password Hashing Implementation**: Confirm `PasswordHasher` is available in codebase
3. **Event Sourcing for Admin**: Should Admin creation emit events like User does?

## Dependencies

### Existing Dependencies
- `com.k12.user.domain.models.UserFactory` - User creation
- `com.k12.user.domain.models.UserRole` - Role enum
- `com.k12.user.domain.models.specialization.admin.Admin` - Admin aggregate
- `com.k12.user.domain.ports.out.UserRepository` - User persistence
- `com.k12.user.domain.ports.out.AdminRepository` - Admin persistence
- `com.k12.tenant.application.service.TenantService` - Tenant validation
- `com.k12.common.infrastructure.security.PasswordHasher` - Password hashing

### New Dependencies to Create
- `TenantAdminService` - Application service
- `CreateTenantAdminRequest` DTO
- `TenantAdminResponse` DTO
- `AdminFactory` (if not exists)
- `TenantAdminError` enum

## Future Enhancements

1. **Bulk Admin Creation**: Create multiple admins in one request
2. **Admin Templates**: Pre-defined permission sets for common admin roles
3. **Admin Listing**: `GET /api/tenants/{tenantId}/admins` to list all tenant admins
4. **Admin Update**: `PUT /api/tenants/{tenantId}/admins/{adminId}` to update permissions
5. **Admin Deactivation**: Endpoint to deactivate tenant admins
6. **Audit Logging**: Log all admin creation events for compliance

## References

- Tenant Resource: `src/main/java/com/k12/tenant/infrastructure/rest/resource/TenantResource.java`
- User Factory: `src/main/java/com/k12/user/domain/models/UserFactory.java`
- Admin Aggregate: `src/main/java/com/k12/user/domain/models/specialization/admin/Admin.java`
- User Role: `src/main/java/com/k12/user/domain/models/UserRole.java`
- Authentication Design: `docs/plans/2026-02-23-authentication-design.md`
- JWT Tenant Context: `docs/plans/2026-02-24-jwt-tenant-context-design.md`
