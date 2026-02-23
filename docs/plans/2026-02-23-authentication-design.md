# Authentication and JWT Login Design

**Date:** 2026-02-23
**Status:** Approved
**Author:** Claude

## Overview

Design and implementation of a login endpoint that generates JWT tokens with user roles. Follows full Event Sourcing and DDD patterns consistent with the existing Tenant module.

## Architecture

### Module Structure

```
user/
├── domain/
│   ├── models/ (already exists: User, UserRole, PasswordHash, etc.)
│   ├── ports/
│   │   ├── out/UserRepository.java (interface exists)
│   │   └── in/AuthenticationUseCase.java (new)
│   ├── services/
│   │   └── AuthenticationService.java (new - domain service)
│   └── error/
│       └── AuthenticationError.java (new)
├── application/
│   ├── AuthenticationApplicationService.java (new)
│   └── dto/
│       ├── LoginRequest.java
│       └── LoginResponse.java
├── infrastructure/
│   ├── persistence/UserRepositoryImpl.java (new - event sourced)
│   ├── security/
│   │   ├── TokenService.java (new - JWT generation)
│   │   └── PasswordMatcher.java (new - BCrypt verification)
│   └── rest/
│       ├── resource/AuthResource.java (new)
│       └── dto/
│           ├── LoginRequestDTO.java
│           └── LoginResponseDTO.java
```

### Key Design Decisions

1. **Event Sourced UserRepository**: Uses user_events table with Kryo serialization (mirroring tenant_repository)
2. **Domain Service**: AuthenticationService handles credential validation in the domain layer
3. **Application Service**: AuthenticationApplicationService orchestrates the login flow
4. **Infrastructure Service**: TokenService generates JWT using RSA keys
5. **BCrypt Password Verification**: Uses existing BCrypt pattern (12 rounds)

## Components

### Domain Layer

#### AuthenticationService (Domain Service)
- Validates user credentials (email + password)
- Uses UserRepository to load User aggregate
- Verifies password using BCrypt
- Checks user status (ACTIVE, SUSPENDED)
- Returns `Result<User, AuthenticationError>` following ROP pattern

#### AuthenticationError
```
- INVALID_CREDENTIALS
- USER_NOT_FOUND
- USER_SUSPENDED
- USER_INACTIVE
```

### Application Layer

#### AuthenticationApplicationService
- Orchestrates login use case
- Calls domain AuthenticationService for validation
- On success, delegates to TokenService for JWT generation
- Returns `Result<LoginResponse, AuthenticationError>`

#### DTOs
**LoginRequest**
- email: String
- password: String

**LoginResponse**
- token: String (JWT)
- user: UserInfo (id, email, name, roles)

### Infrastructure Layer

#### UserRepositoryImpl
- Implements UserRepository using jOOQ
- Event-sourced: loads events from user_events, reconstructs User via UserReconstructor
- Mirrors TenantRepositoryImpl pattern (Kryo serialization)
- Methods: `save()`, `findById()`, `findByEmailAddress()`

#### TokenService
- Generates JWT RS256 tokens using RSA keys
- Includes claims: sub (userId), email, roles, tenantId, iss, exp, iat
- Reads private-key.pem for signing
- 24-hour token expiration

#### PasswordMatcher
- Verifies BCrypt password hashes
- Simple utility class wrapping BCrypt.checkpw()

#### AuthResource (REST Endpoint)
- `POST /api/auth/login`
- Accepts LoginRequestDTO
- Returns 200 + LoginResponseDTO on success
- Returns 401 with error details on failure

## Data Flow

### Login Request Flow

1. **Client → AuthResource**
   - POST /api/auth/login
   - Body: `{ email: "admin@k12.com", password: "admin123" }`

2. **AuthResource → AuthenticationApplicationService**
   - Validates DTO, forwards request

3. **AuthenticationApplicationService → AuthenticationService (Domain)**
   - Validates credentials

4. **AuthenticationService → UserRepository**
   - findByEmailAddress("admin@k12.com")

5. **UserRepositoryImpl**
   - SELECT * FROM users WHERE email = ?
   - Loads events from user_events for this user_id
   - Reconstructs User aggregate via UserReconstructor
   - Returns Optional<User>

6. **AuthenticationService**
   - If user not found → Result.failure(USER_NOT_FOUND)
   - If user.status != ACTIVE → Result.failure(USER_SUSPENDED)
   - PasswordMatcher.verify(password, user.passwordHash())
   - If not match → Result.failure(INVALID_CREDENTIALS)
   - If match → Result.success(user)

7. **AuthenticationApplicationService (on success)**
   - TokenService.generateToken(user)
   - Creates LoginResponse with token + user info
   - Result.success(LoginResponse)

8. **AuthResource**
   - Returns 200 OK with LoginResponseDTO
   - OR (on failure) Returns 401 Unauthorized with ErrorResponseDTO

### Event Sourcing

**UserRepositoryImpl.load():**
1. Query user_events for user_id, order by version ASC
2. Deserialize each event_data using KryoEventSerializer
3. Apply events to initial User via UserReconstructor
4. Return reconstructed User

**Note:** No events are written during login - login is a read-only operation.

## JWT Token Structure

### Token Claims

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@k12.com",
  "roles": ["SUPER_ADMIN"],
  "tenantId": "tenant-uuid-here",
  "iss": "k12-api",
  "iat": 1740277600,
  "exp": 1740364000
}
```

| Claim | Description | Example |
|-------|-------------|---------|
| `sub` | User ID (standard JWT subject) | UUID |
| `email` | User's email address | "admin@k12.com" |
| `roles` | Array of user roles | ["SUPER_ADMIN", "ADMIN"] |
| `tenantId` | Associated tenant ID | UUID |
| `iss` | Token issuer | "k12-api" (from config) |
| `iat` | Issued at timestamp | Unix epoch |
| `exp` | Expiration timestamp | Unix epoch (iat + 24hrs) |

### Token Configuration

- Algorithm: RS256 (RSA signature with SHA-256)
- Private Key: `src/main/resources/keys/private-key.pem`
- Expiration: 24 hours (86400 seconds)
- Dependencies: SmallRye JWT

## Error Handling

### Domain Errors

```java
public sealed interface AuthenticationError {
    record InvalidCredentials(String message) implements AuthenticationError;
    record UserNotFound(String message) implements AuthenticationError;
    record UserSuspended(String message) implements AuthenticationError;
    record UserInactive(String message) implements AuthenticationError;
}
```

### HTTP Response Mapping

| Error | HTTP Status | Response Body |
|-------|-------------|---------------|
| `InvalidCredentials` | 401 Unauthorized | `{ "error": "INVALID_CREDENTIALS", "message": "Invalid email or password" }` |
| `UserNotFound` | 401 Unauthorized | `{ "error": "USER_NOT_FOUND", "message": "No account found with this email" }` |
| `UserSuspended` | 403 Forbidden | `{ "error": "USER_SUSPENDED", "message": "Account has been suspended" }` |
| `UserInactive` | 403 Forbidden | `{ "error": "USER_INACTIVE", "message": "Account is not active" }` |
| Validation errors | 400 Bad Request | `{ "error": "VALIDATION_ERROR", "message": "...", "fields": {...} }` |

**Security Note:** Returns 401 for both `InvalidCredentials` and `UserNotFound` to prevent email enumeration.

## Testing Strategy

### Unit Tests

- **AuthenticationServiceTest**: Mock UserRepository, test all success/failure scenarios
- **TokenServiceTest**: Mock private key, test token generation and claims
- **PasswordMatcherTest**: Test BCrypt verification

### Integration Tests

- **UserRepositoryImplIntegrationTest**: Test loading users from database with event reconstruction
- **AuthResourceIntegrationTest**: End-to-end login flow with @TestHTTPResource

### Test Data

```sql
-- Test user: test@k12.com / password123
INSERT INTO users (id, email, password_hash, roles, status, name, version)
VALUES (
    'test-user-uuid',
    'test@k12.com',
    '$2b$12$...',
    'TEACHER',
    'ACTIVE',
    'Test Teacher',
    1
);
```

## Dependencies

### Existing (Already Configured)
- Quarkus with JAX-RS
- PostgreSQL with Flyway
- SmallRye JWT (RS256)
- RSA keys in `/resources/keys/`
- jOOQ for database access
- BCrypt for password hashing

### New Required
- Minimal new dependencies (use existing Quarkus stack)

## Implementation Notes

1. **Tenant Association**: User domain will need a `tenantId` field added to support multi-tenancy
2. **Kryo Serialization**: Reuse existing `KryoEventSerializer` from tenant module
3. **Database Migration**: May need to add `tenant_id` column to users table
4. **OpenAPI Documentation**: All endpoints will have full OpenAPI docs
