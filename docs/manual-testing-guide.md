# Manual Testing Guide: Create Tenant Admin Endpoint

## Overview
This guide provides step-by-step instructions for manually testing the `POST /api/tenants/{tenantId}/admins` endpoint.

## Prerequisites

1. Application running: `mvn quarkus:dev`
2. SUPER_ADMIN user account with valid credentials
3. Test tenant ID (create one via POST /api/tenants if needed)
4. Tool for API testing: curl, Postman, or HTTPie

## Test Scenarios

### 1. Successful Admin Creation (201 Created)

**Request:**
```bash
# 1. Login as SUPER_ADMIN to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"superadmin@k12.com","password":"your-password"}' | jq -r '.token')

# 2. Create a test tenant (if needed)
TENANT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Test Tenant",
    "subdomain": "testtenant"
  }')

TENANT_ID=$(echo $TENANT_RESPONSE | jq -r '.tenantId')
echo "Tenant ID: $TENANT_ID"

# 3. Create tenant admin
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@testtenant.com",
    "password": "SecurePass123",
    "name": "Test Tenant Admin",
    "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"]
  }' | jq '.'
```

**Expected Response (201):**
```json
{
  "userId": "uuid",
  "email": "admin@testtenant.com",
  "name": "Test Tenant Admin",
  "role": "ADMIN",
  "tenantId": "$TENANT_ID",
  "adminId": "uuid",
  "permissions": ["USER_MANAGEMENT", "COURSE_MANAGEMENT", "VIEW_REPORTS"],
  "createdAt": "2024-02-26T10:30:00Z"
}
```

### 2. Invalid Email Format (400 Bad Request)

**Request:**
```bash
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "not-an-email",
    "password": "SecurePass123",
    "name": "Test Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'
```

**Expected Response (400):**
```json
{
  "error": "INVALID_EMAIL",
  "message": "Invalid email format"
}
```

### 3. Password Too Short (400 Bad Request)

**Request:**
```bash
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@test.com",
    "password": "short",
    "name": "Test Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'
```

**Expected Response (400):**
```json
{
  "error": "INVALID_PASSWORD",
  "message": "Password must be at least 8 characters"
}
```

### 4. Empty Permissions (400 Bad Request)

**Request:**
```bash
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@test.com",
    "password": "SecurePass123",
    "name": "Test Admin",
    "permissions": []
  }'
```

**Expected Response (400):**
```json
{
  "error": "INVALID_PERMISSIONS",
  "message": "At least one permission is required"
}
```

### 5. Duplicate Email (409 Conflict)

**Request:**
```bash
# Create admin first
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@test.com",
    "password": "SecurePass123",
    "name": "Test Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'

# Try to create again with same email
curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@test.com",
    "password": "AnotherPass123",
    "name": "Another Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'
```

**Expected Response (409):**
```json
{
  "error": "EMAIL_ALREADY_EXISTS",
  "message": "Email already exists"
}
```

### 6. Tenant Not Found (404 Not Found)

**Request:**
```bash
curl -X POST "http://localhost:8080/api/tenants/00000000-0000-0000-0000-000000000000/admins" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@fake.com",
    "password": "SecurePass123",
    "name": "Fake Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'
```

**Expected Response (404):**
```json
{
  "error": "TENANT_NOT_FOUND",
  "message": "Tenant not found"
}
```

### 7. Missing SUPER_ADMIN Role (403 Forbidden)

**Request:**
```bash
# Get token for regular user (not SUPER_ADMIN)
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"regular@user.com","password":"user-password"}' | jq -r '.token')

curl -X POST "http://localhost:8080/api/tenants/$TENANT_ID/admins" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@test.com",
    "password": "SecurePass123",
    "name": "Test Admin",
    "permissions": ["USER_MANAGEMENT"]
  }'
```

**Expected Response (403):**
```json
{
  "error": "FORBIDDEN",
  "message": "SUPER_ADMIN role required"
}
```

## Database Verification

After successful admin creation, verify in database:

```bash
# Check users table
psql -h localhost -U k12_user -d k12_db -c \
  "SELECT id, email, roles, tenant_id FROM users WHERE email = 'admin@testtenant.com';"

# Check admin tables
psql -h localhost -U k12_user -d k12_db -c \
  "SELECT * FROM admin WHERE admin_id = (SELECT id FROM users WHERE email = 'admin@testtenant.com');"
```

## Testing Checklist

- [ ] 201 Created - Valid request
- [ ] 400 Bad Request - Invalid email
- [ ] 400 Bad Request - Short password
- [ ] 400 Bad Request - Blank name
- [ ] 400 Bad Request - Empty permissions
- [ ] 409 Conflict - Duplicate email
- [ ] 404 Not Found - Invalid tenant
- [ ] 403 Forbidden - Non-SUPER_ADMIN token
- [ ] Database - User created with correct tenant_id
- [ ] Database - Admin aggregate created

## Troubleshooting

### Compilation Errors
If the application won't start due to compilation errors in TenantAdminService, TenantAdminResponse, or related files:
1. Check the specific error messages
2. Ensure all dependencies are correctly imported
3. Verify method signatures match interfaces
4. Check Result type API usage

### Authentication Issues
If you get 401 Unauthorized:
- Verify SUPER_ADMIN user exists
- Check JWT token generation
- Ensure token is not expired
- Verify Authorization header format: `Bearer <token>`

### Validation Issues
If validation doesn't work:
- Check @Valid annotation is present on request parameter
- Verify validation constraints on DTO fields
- Ensure Jakarta validation is enabled in Quarkus

## Notes

- All requests require valid JWT token from SUPER_ADMIN user
- Password is hashed before storage (never returned in responses)
- Email must be unique across entire system (not just per tenant)
- Permissions must be valid Permission enum values
- Created User always has role ADMIN (hardcoded in service)
