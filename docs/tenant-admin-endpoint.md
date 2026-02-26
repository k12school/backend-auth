# Create Tenant Admin Endpoint

## Overview

Create tenant-specific administrator users with the ADMIN role and assigned permissions through a SUPER_ADMIN-only REST endpoint.

## Endpoint

**POST** `/api/tenants/{tenantId}/admins`

### Authorization

- Requires `SUPER_ADMIN` role
- JWT token must include SUPER_ADMIN in roles claim
- TenantResource class-level annotation: `@RolesAllowed("SUPER_ADMIN")`

### Request

```json
{
  "email": "admin@tenant.com",
  "password": "SecurePass123",
  "name": "Tenant Administrator",
  "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"]
}
```

**Request Fields:**
- `email` (string, required): Admin email address with validation
- `password` (string, required): Min 8 characters, will be hashed
- `name` (string, required): Admin full name
- `permissions` (array of strings, required): At least one permission required

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

**Response Fields:**
- `userId`: Unique user identifier
- `email`: User email address
- `name`: User full name
- `role`: Always "ADMIN" (hardcoded in service)
- `tenantId`: Associated tenant identifier
- `adminId`: Admin identifier (same as userId)
- `permissions`: Set of assigned permissions
- `createdAt`: ISO 8601 timestamp

## Available Permissions

See `Permission` enum in:
`src/main/java/com/k12/user/domain/models/specialization/admin/valueobjects/Permission.java`

Full list includes:
- USER_MANAGEMENT
- COURSE_MANAGEMENT
- TEACHER_MANAGEMENT
- STUDENT_MANAGEMENT
- PARENT_MANAGEMENT
- SYSTEM_SETTINGS
- REPORTS_VIEW
- REPORTS_EXPORT
- MANAGE_USERS
- CREATE_USER
- DELETE_USER
- SUSPEND_USER
- VIEW_ALL_USERS
- MODIFY_USER_ROLES
- MANAGE_COURSES
- CREATE_COURSE
- UPDATE_COURSE
- DELETE_COURSE
- VIEW_ALL_COURSES
- MANAGE_ENROLLMENTS
- ENROLL_STUDENT
- DROP_STUDENT
- VIEW_ALL_ENROLLMENTS
- MANAGE_GRADING
- VIEW_ALL_GRADES
- MODIFY_GRADES
- CREATE_ASSIGNMENT
- MANAGE_TEACHERS
- VIEW_TEACHER_ASSIGNMENTS
- MANAGE_PARENTS
- LINK_PARENT_STUDENT
- VIEW_PARENT_LINKS
- MANAGE_TENANTS
- MANAGE_ROLES
- VIEW_ALL_TENANTS
- VIEW_REPORTS
- EXPORT_DATA
- MANAGE_SETTINGS
- MANAGE_PERMISSIONS
- VIEW_AUDIT_LOGS
- GRADE_ASSIGNMENT

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

## Implementation Details

### Architecture

```
POST /api/tenants/{tenantId}/admins
    │
    ▼
TenantResource.createTenantAdmin()
    │
    ▼
TenantAdminService.createTenantAdmin()
    │
    ├─► Validate tenant exists
    ├─► Check email uniqueness
    ├─► Validate input (email, password, name, permissions)
    ├─► Hash password
    ├─► Create User with ADMIN role (via UserFactory)
    ├─► Save User (with tenant_id)
    ├─► Create Admin aggregate (via AdminFactory)
    ├─► Save Admin
    └─► Build response
```

### Key Design Decisions

1. **Role Hardcoded as ADMIN**: The service hardcodes `Set.of(UserRole.ADMIN)` - the request cannot specify other roles

2. **Transactional Creation**: Entire operation wrapped in `@Transactional` - if admin creation fails, user creation is rolled back

3. **Dual Aggregate Creation**: Creates both User entity and Admin specialization atomically

4. **Permission Validation**: Permissions are validated at creation time via AdminFactory

### Components

- **TenantResource**: JAX-RS endpoint with OpenTelemetry tracing
- **TenantAdminService**: Application service orchestrating the creation flow
- **UserFactory**: Domain factory for User creation
- **AdminFactory**: Domain factory for Admin creation
- **UserRepository**: User persistence
- **AdminRepository**: Admin persistence
- **TenantService**: Tenant validation
- **PasswordHasher**: Password hashing utility

## Testing

### Unit Tests
- `CreateTenantAdminRequestTest`: Validation tests for request DTO

### Integration Tests
- `TenantResourceCreateAdminIntegrationTest`: HTTP endpoint tests (TODO: requires JWT setup)

### Manual Testing
See `docs/manual-testing-guide.md` for comprehensive manual testing procedures.

## Security Considerations

1. **Authorization**: Only SUPER_ADMIN role can access this endpoint
2. **Password Security**: Passwords are hashed using BCrypt before storage
3. **Email Uniqueness**: Email must be unique across entire system
4. **Tenant Isolation**: User associated with specific tenant via tenant_id
5. **Permission Validation**: Only valid Permission enum values accepted

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    roles VARCHAR(255)[] NOT NULL,
    status VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    tenant_id UUID REFERENCES tenants(id),
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Admin Table
```sql
CREATE TABLE admin (
    admin_id UUID PRIMARY KEY REFERENCES users(id),
    permissions JSONB NOT NULL,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## Known Issues

As of implementation completion:

1. **Compilation Errors**:
   - Result.isError()/getSuccess() API usage needs verification
   - TenantAdminError enum structure needs alignment
   - TenantAdminResponse UUID to String conversion needed

2. **Integration Tests**: Require JWT token generation setup for testing

3. **Error Response Mapping**: Uses custom toErrorResponse in TenantResource vs. centralized ErrorResponseMapper

## Future Enhancements

1. **Bulk Admin Creation**: Create multiple admins in one request
2. **Admin Templates**: Pre-defined permission sets for common admin roles
3. **Admin Listing**: `GET /api/tenants/{tenantId}/admins` to list tenant admins
4. **Admin Update**: `PUT /api/tenants/{tenantId}/admins/{adminId}` to update permissions
5. **Admin Deactivation**: Endpoint to deactivate tenant admins
6. **Audit Logging**: Log all admin creation events for compliance

## Related Documentation

- Design Document: `docs/plans/2026-02-26-create-tenant-admin-endpoint-design.md`
- Implementation Plan: `docs/plans/2026-02-26-create-tenant-admin-implementation.md`
- Manual Testing: `docs/manual-testing-guide.md`
- OpenAPI Verification: `docs/openapi-verification-checklist.md`
