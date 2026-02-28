# Users Management System Design

**Date:** 2026-02-28
**Author:** Claude (with user requirements)
**Status:** Approved

## Overview

Implement a complete Users management system with tenant associations and role-based specializations (Teacher, Parent, Student).

### Key Requirements

1. **Tenant Association**: Users are associated to tenants via `tenant_id` (from JWT)
2. **Role-Based Creation**: Only ADMIN role can create users
3. **Admin Tenant Constraint**: Users are created in the tenant from JWT (automatic, not overrideable)
4. **Specializations**: Users can have specializations: ADMIN, TEACHER, PARENT, STUDENT
5. **Tenant Isolation**: ADMIN can only create users in their own tenant

## Architecture

### User Hierarchy

```
SUPER_TENANT
  └─ SUPER_ADMIN (creates tenants & tenant admins)

Regular Tenants (created by SUPER_ADMIN)
  └─ ADMIN (created by SUPER_ADMIN for specific tenant)
      └─ TEACHER, PARENT, STUDENT (created by tenant's ADMIN)
```

### Authorization Flow

1. **SUPER_ADMIN** → Creates tenant → Creates tenant admin for that tenant
2. **Tenant ADMIN** → Creates users for their tenant (tenant_id from JWT)
3. **Tenant Scoping** → Users automatically assigned to ADMIN's tenant

## API Design

### Endpoint

```
POST /api/users
Authorization: Bearer <JWT>
Roles Allowed: ADMIN
```

### Request Structure

**Common Fields (all types):**
```json
{
  "type": "TEACHER | PARENT | STUDENT",
  "email": "string (required)",
  "password": "string (required, min 8 chars)",
  "name": "string (required)"
}
```

**TEACHER Specific Fields:**
```json
{
  "employeeId": "string (required, globally unique)",
  "department": "string",
  "hireDate": "ISO date (YYYY-MM-DD)"
}
```

**PARENT Specific Fields:**
```json
{
  "phoneNumber": "string",
  "address": "string",
  "emergencyContact": "string"
}
```

**STUDENT Specific Fields:**
```json
{
  "studentId": "string (required, globally unique)",
  "gradeLevel": "string (required)",
  "dateOfBirth": "ISO date (required, not in future)",
  "guardianId": "UUID (must reference existing parent)"
}
```

### Response (201 Created)

```json
{
  "userId": "UUID",
  "tenantId": "UUID",
  "email": "string",
  "name": "string",
  "role": "USER",
  "status": "ACTIVE",
  "type": "TEACHER",
  "employeeId": "string",
  "department": "string",
  "hireDate": "ISO date",
  "createdAt": "ISO timestamp"
}
```

## Domain Model

### Base User Aggregate

```java
User {
  UserId userId
  EmailAddress email
  PasswordHash passwordHash
  Set<UserRole> roles
  UserStatus status
  UserName name
  TenantId tenantId  // NEW: Tenant association
}
```

### Specialization Aggregates

**Teacher:**
```java
Teacher {
  TeacherId teacherId  // wraps UserId
  String employeeId
  String department
  LocalDate hireDate
  Instant createdAt
}
```

**Parent:**
```java
Parent {
  ParentId parentId  // wraps UserId
  String phoneNumber
  String address
  String emergencyContact
  Instant createdAt
}
```

**Student:**
```java
Student {
  StudentId studentId  // wraps UserId
  String studentId
  String gradeLevel
  LocalDate dateOfBirth
  ParentId guardianId  // links to Parent
  Instant createdAt
}
```

### Repositories

- `UserRepository` - Update to include tenantId
- `TeacherRepository` - NEW
- `ParentRepository` - NEW
- `StudentRepository` - NEW

### Factories

- `UserFactory` - Create base users
- `TeacherFactory`, `ParentFactory`, `StudentFactory` - Create specializations

## Database Schema

### Users Table Update

```sql
-- Add tenant_id to users table
ALTER TABLE users ADD COLUMN tenant_id UUID NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant
  FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_users_tenant ON users(tenant_id);

-- Add tenant_id to user_events for event sourcing
ALTER TABLE user_events ADD COLUMN tenant_id UUID NOT NULL;
```

### Teachers Table

```sql
CREATE TABLE teachers (
  user_id UUID PRIMARY KEY,
  employee_id VARCHAR(100) UNIQUE NOT NULL,
  department VARCHAR(200),
  hire_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_teachers_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);
```

### Parents Table

```sql
CREATE TABLE parents (
  user_id UUID PRIMARY KEY,
  phone_number VARCHAR(50),
  address TEXT,
  emergency_contact VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_parents_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);
```

### Students Table

```sql
CREATE TABLE students (
  user_id UUID PRIMARY KEY,
  student_id VARCHAR(100) UNIQUE NOT NULL,
  grade_level VARCHAR(50) NOT NULL,
  date_of_birth DATE NOT NULL,
  guardian_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_students_guardian FOREIGN KEY (guardian_id)
    REFERENCES parents(user_id)
);
```

## Application Service

### UserService

```java
@ApplicationScoped
@RolesAllowed("ADMIN")
public class UserService {

  public Result<UserWithSpecialization, UserError> createUser(
      CreateUserRequest request) {

    // 1. Extract tenantId from JWT
    TenantId tenantId = SecurityContext.getCurrentTenantId()
      .orElseThrow(() -> new IllegalStateException("No tenant in JWT"));

    // 2. Validate email uniqueness within tenant
    if (userRepository.existsByEmailInTenant(request.email(), tenantId)) {
      return Result.failure(UserError.EmailError.EMAIL_ALREADY_EXISTS);
    }

    // 3. Create base user with tenant association
    Result<User, UserError> userResult = UserFactory.create(
      request.email(),
      request.password(),
      Set.of(UserRole.USER),
      tenantId,
      request.name()
    );

    // 4. Create specialization based on type
    User user = userResult.get();
    switch (request.type()) {
      case TEACHER:
        teacherRepository.create(teacherFactory.create(...));
        break;
      case PARENT:
        parentRepository.create(parentFactory.create(...));
        break;
      case STUDENT:
        // Validate guardian exists
        if (!parentRepository.existsById(request.guardianId())) {
          return Result.failure(UserError.ValidationError.GUARDIAN_NOT_FOUND);
        }
        studentRepository.create(studentFactory.create(...));
        break;
    }

    // 5. Save user (within transaction)
    userRepository.save(user);

    return Result.success(new UserWithSpecialization(user, specialization));
  }
}
```

## Authorization & Security

### JWT Token Structure

```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "roles": ["ADMIN"],
  "tenantId": "tenant-uuid",  // Used for tenant scoping
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Security Rules

1. **JWT Filter** → Extracts `tenantId` and `roles` → Sets in `SecurityContext`
2. **@RolesAllowed("ADMIN")** → Quarkus validates JWT has ADMIN role
3. **Tenant Validation** → Service validates operations against JWT's tenantId
4. **Automatic Assignment** → Created users get JWT's tenantId (cannot override)

### Cross-Tenant Protection

- Users cannot be created for a different tenant than JWT's tenantId
- Email uniqueness checked within tenant scope
- All queries scoped to JWT's tenant

## Error Handling

### Validation Errors

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| VALIDATION_ERROR | 400 | Invalid request data |
| EMAIL_ALREADY_EXISTS | 409 | Email already exists in tenant |
| GUARDIAN_NOT_FOUND | 404 | Referenced guardian does not exist |
| DUPLICATE_EMPLOYEE_ID | 409 | Employee ID already exists |
| DUPLICATE_STUDENT_ID | 409 | Student ID already exists |

### Authorization Errors

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| INSUFFICIENT_PERMISSIONS | 403 | User lacks ADMIN role |
| NO_TENANT_IN_CONTEXT | 401 | JWT missing tenantId |

### Transaction Management

- User creation and specialization creation in single `@Transactional` boundary
- Rollback on any failure
- Event sourcing for User aggregate (user_events table)

## Implementation Components

### New Files to Create

**Domain Models:**
- `src/main/java/com/k12/user/domain/models/specialization/teacher/Teacher.java`
- `src/main/java/com/k12/user/domain/models/specialization/parent/Parent.java`
- `src/main/java/com/k12/user/domain/models/specialization/student/Student.java`
- `src/main/java/com/k12/user/domain/models/specialization/teacher/TeacherId.java`
- `src/main/java/com/k12/user/domain/models/specialization/parent/ParentId.java`
- `src/main/java/com/k12/user/domain/models/specialization/student/StudentId.java`

**Repositories:**
- `src/main/java/com/k12/user/infrastructure/persistence/TeacherRepositoryImpl.java`
- `src/main/java/com/k12/user/infrastructure/persistence/ParentRepositoryImpl.java`
- `src/main/java/com/k12/user/infrastructure/persistence/StudentRepositoryImpl.java`

**Ports:**
- `src/main/java/com/k12/user/domain/ports/out/TeacherRepository.java`
- `src/main/java/com/k12/user/domain/ports/out/ParentRepository.java`
- `src/main/java/com/k12/user/domain/ports/out/StudentRepository.java`

**Application Service:**
- `src/main/java/com/k12/user/application/service/UserService.java`

**REST Resource:**
- `src/main/java/com/k12/user/infrastructure/rest/resource/UserResource.java`

**DTOs:**
- `src/main/java/com/k12/user/infrastructure/rest/dto/CreateUserRequest.java`
- `src/main/java/com/k12/user/infrastructure/rest/dto/UserResponse.java`
- `src/main/java/com/k12/user/infrastructure/rest/dto/CreateTeacherRequest.java`
- `src/main/java/com/k12/user/infrastructure/rest/dto/CreateParentRequest.java`
- `src/main/java/com/k12/user/infrastructure/rest/dto/CreateStudentRequest.java`

### Files to Modify

**Domain:**
- `src/main/java/com/k12/user/domain/models/User.java` - Add tenantId field
- `src/main/java/com/k12/user/domain/models/UserFactory.java` - Support tenantId
- `src/main/java/com/k12/user/domain/ports/out/UserRepository.java` - Add existsByEmailInTenant()

**Infrastructure:**
- `src/main/java/com/k12/user/infrastructure/persistence/UserRepositoryImpl.java` - Update for tenantId
- `src/main/java/com/k12/tenant/infrastructure/persistence/KryoEventSerializer.java` - Add tenantId to UserCreated event

**Migrations:**
- `src/main/resources/db/migration/V10__Add_Tenant_To_Users.sql`
- `src/main/resources/db/migration/V11__Create_Specialization_Tables.sql`

## Testing Strategy

1. **Unit Tests:**
   - UserFactory with tenantId
   - Specialization factories
   - Repository CRUD operations

2. **Integration Tests:**
   - User creation endpoint with different specializations
   - Tenant isolation (ADMIN from tenant A cannot create users for tenant B)
   - Authorization (non-ADMIN cannot create users)
   - Validation (duplicate emails, invalid guardian, etc.)

3. **Manual Testing:**
   - Use Redoc UI at http://localhost:8082 to test endpoints
   - Verify JWT contains correct tenantId
   - Test cross-tenant access prevention

## Success Criteria

✅ ADMIN can create TEACHER, PARENT, STUDENT users for their tenant
✅ Created users automatically assigned to tenant from JWT
✅ Email uniqueness enforced within tenant scope
✅ Specialization-specific validations work correctly
✅ Cross-tenant user creation is prevented
✅ Event sourcing works for User aggregate with tenantId
✅ API returns full user object with specialization data

## Open Questions

None - design approved.
