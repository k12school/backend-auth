# UserService and REST Endpoints Design

**Date:** 2026-02-28
**Status:** Approved
**Goal:** Implement service layer and REST API for user management with role-based specializations

---

## Overview

Service layer and REST API for managing users with role-based specializations (Teacher, Parent, Student, Admin). Supports full CRUD operations with role isolation, soft delete, and separate role change operations.

### Architecture

**Three-Layer Hexagonal Architecture:**

1. **Domain Layer** (Already exists)
   - User aggregate with value objects
   - Specialization aggregates (Teacher, Parent, Student, Admin)
   - Factories for user creation
   - Domain errors and Result types

2. **Service Layer** (UserService - NEW)
   - Coordinates UserRepository + specialization repositories
   - Business logic for CRUD operations
   - Role-based data transformations
   - Error handling with Result types

3. **REST Layer** (UserResource - NEW)
   - JAX-RS endpoints with OpenAPI docs
   - Request/response DTOs (already created)
   - Bean Validation
   - Authentication via JWT

---

## Components

### UserService

**Location:** `src/main/java/com/k12/user/application/UserService.java`

**Responsibilities:**
- Coordinate between UserRepository and specialization repositories
- Validate business rules (email uniqueness, role compatibility)
- Handle password hashing for new users
- Orchestrate role changes with data migration
- Manage soft delete (status change)

**Methods:**

```java
public Result<UserResponse, UserError> createUser(CreateUserRequest request)
public Result<UserResponse, UserError> getUserById(UserId id)
public Result<List<UserResponse>, UserError> listUsers(UserRole role, TenantId tenantId, UserStatus status)
public Result<UserResponse, UserError> updateUserFields(UserId id, UpdateUserRequest request)
public Result<UserResponse, UserError> changeUserRole(UserId id, ChangeRoleRequest request)
public Result<Void, UserError> softDeleteUser(UserId id)
```

---

### REST Endpoints

**Location:** `src/main/java/com/k12/user/infrastructure/rest/resource/UserResource.java`

**Endpoints:**

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | `/api/users` | Create user | CreateUserRequest | UserResponse (201) |
| GET | `/api/users/{id}` | Get user by ID | - | UserResponse (200) |
| GET | `/api/users` | List/filter users | Query params | List<UserResponse> (200) |
| PATCH | `/api/users/{id}` | Update fields | UpdateUserRequest | UserResponse (200) |
| PUT | `/api/users/{id}/role` | Change role | ChangeRoleRequest | UserResponse (200) |
| DELETE | `/api/users/{id}` | Soft delete | - | 204 No Content |

**Query Parameters for GET /api/users:**
- `role` (optional) - Filter by user role
- `tenantId` (optional) - Filter by tenant
- `status` (optional) - Filter by status (ACTIVE, SUSPENDED, DELETED)

---

## Data Flow

### Create User Flow

1. Validate request (Bean Validation: @Email, @Size, @NotBlank)
2. Check email uniqueness → Error if exists
3. Hash password via PasswordHasher
4. Create base User via UserFactory
5. Create specialization via role-specific Factory (TeacherFactory, ParentFactory, etc.)
6. Save User event via UserRepository (event sourcing)
7. Save specialization via respective repository (jOOQ)
8. Return UserResponse with role-specific data

### Update User Fields Flow

1. Load existing user and specialization
2. Validate updated fields
3. Update domain objects via appropriate methods
4. Save updates to repositories
5. Return updated UserResponse

### Role Change Flow

1. Validate new role and required specialization data
2. Load existing user and current specialization
3. Soft-delete old specialization (if exists)
4. Update User domain model with new role
5. Create new specialization via role-specific Factory
6. Save updates to repositories (User + new specialization)
7. Return new UserResponse

### Soft Delete Flow

1. Load existing user
2. Update status to DELETED
3. Save User event via UserRepository
4. Return success

---

## Error Handling

**UserError Types:**

```java
public sealed interface UserError {
    record ValidationError(String message) implements UserError
    record ConflictError(String message) implements UserError
    record NotFoundError(String message) implements UserError
    record PersistenceError(String message) implements UserError
}
```

**Error Mapping:**
- Email already exists → 409 Conflict
- User not found → 404 Not Found
- Invalid input → 400 Bad Request
- Database error → 500 Internal Server Error

Uses Result<T, E> pattern - no exceptions for business logic.

---

## Security

**Authentication:**
- All endpoints require JWT token
- Extract tenant ID from JWT claim
- Validate user has permission (SUPER_ADMIN or ADMIN role)

**Authorization:**
```java
@RolesAllowed({ "SUPER_ADMIN", "ADMIN" })
public class UserResource {
    // Tenant-scoped: users can only manage users in their tenant
    // SUPER_ADMIN can manage across all tenants
}
```

**Tenant Isolation:**
- Filter users by tenant from JWT
- SUPER_ADMIN bypasses tenant filter (can see all users)
- Users cannot access users from other tenants

---

## Request/Response DTOs

**Already Created:**
- `CreateUserRequest` - with role-based data (TeacherData, ParentData, StudentData)
- `TeacherResponse` - with employeeId, department, hireDate
- `ParentResponse` - with phoneNumber, address, emergencyContact
- `StudentResponse` - with studentNumber, gradeLevel, dateOfBirth, guardianId
- `CreateUserRequest.UserRole` - wrapper for role string validation

**Need to Create:**
- `UpdateUserRequest` - for field updates within current role
- `ChangeRoleRequest` - for role changes with new specialization data
- `UserResponse` - base response (userId, email, name, role, status, etc.)

---

## Implementation Approach

**Phase 1: Foundation**
1. Create UserError types
2. Create UserService skeleton with all method signatures
3. Create UserResponse DTO
4. Create UpdateUserRequest and ChangeRoleRequest DTOs

**Phase 2: Service Layer**
5. Implement createUser method
6. Implement getUserById and listUsers
7. Implement updateUserFields
8. Implement changeUserRole
9. Implement softDeleteUser

**Phase 3: REST Layer**
10. Create UserResource with all endpoints
11. Add OpenAPI annotations
12. Add security annotations (@RolesAllowed)
13. Implement error response mapping

**Phase 4: Testing**
14. Write unit tests for UserService
15. Write integration tests for UserResource
16. Test with Redoc/manual testing

---

## Success Criteria

- [ ] All CRUD operations working via REST API
- [ ] Role-based data properly stored/retrieved
- [ ] Email uniqueness validation enforced
- [ ] Soft delete working (status change to DELETED)
- [ ] Role changes working with data migration
- [ ] Tenant isolation enforced (except SUPER_ADMIN)
- [ ] 100% test coverage for new code
- [ ] OpenAPI documentation complete
- [ ] All endpoints secured with JWT authentication

---

**Next Step:** Invoke writing-plans skill to create detailed implementation plan.
