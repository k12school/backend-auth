# OpenAPI Documentation Verification Checklist
## Create Tenant Admin Endpoint

**Endpoint:** `POST /api/tenants/{id}/admins`
**Task:** Task 12 - Verify OpenAPI documentation is correctly exposed
**Date:** 2026-02-26

---

## Overview

This document provides a comprehensive verification checklist for ensuring the OpenAPI documentation for the `createTenantAdmin` endpoint is correctly exposed through the Swagger UI.

---

## Access Information

### Swagger UI URL
- **Development:** `http://localhost:8080/q/swagger-ui`
- **Alternative:** `http://localhost:8080/q/openapi` (for raw OpenAPI JSON)

### Prerequisites
- Application must be running without compilation errors
- User must authenticate with SUPER_ADMIN role to access secured endpoints
- Swagger UI must be enabled in application configuration

---

## Step-by-Step Verification Guide

### 1. Access Swagger UI
- [ ] Start the application: `./gradlew quarkusDev`
- [ ] Navigate to: `http://localhost:8080/q/swagger-ui`
- [ ] Verify the page loads successfully
- [ ] Look for "Tenants" section in the API documentation

### 2. Locate the Endpoint
- [ ] Find the "Tenants" tag/section in Swagger UI
- [ ] Expand the Tenants section
- [ ] Locate `POST /api/tenants/{id}/admins` endpoint
- [ ] Verify the endpoint summary: "Create a tenant administrator"

### 3. Verify Endpoint Documentation

#### 3.1 Operation Summary and Description
- [ ] **Summary:** "Create a tenant administrator"
- [ ] **Description:** "Creates a new ADMIN user and Admin specialization for a specific tenant"

#### 3.2 Path Parameters
- [ ] Parameter name: `id`
- [ ] Parameter location: path
- [ ] Parameter required: true
- [ ] Parameter example: `123e4567-e89b-12d3-a456-426614174000`
- [ ] Parameter description: "Tenant ID"

#### 3.3 Request Body Schema
- [ ] Content-Type: `application/json`
- [ ] Schema name: `CreateTenantAdminRequest`
- [ ] Description: "Request payload for creating a tenant administrator"

**Request Body Fields:**
- [ ] `email` (string, required)
  - Description: "Admin email address"
  - Example: "admin@tenant.com"
  - Format: email validation
- [ ] `password` (string, required)
  - Description: "Admin password (will be hashed)"
  - Example: "SecurePass123"
  - Min length: 8 characters
- [ ] `name` (string, required)
  - Description: "Admin full name"
  - Example: "Tenant Administrator"
- [ ] `permissions` (array of Permission, required)
  - Description: "Set of permissions for this admin"
  - Min items: 1

#### 3.4 Response Codes and Schemas

**200 Range Responses:**
- [ ] **201 Created**
  - Description: "Admin created successfully"
  - Content-Type: `application/json`
  - Schema: `TenantAdminResponse`
  - Verify all fields are documented (see below)

**400 Range Responses:**
- [ ] **400 Bad Request**
  - Description: "Invalid request data"
  - Schema: `ErrorResponse`
  - Example errors: INVALID_EMAIL, INVALID_PASSWORD, INVALID_NAME, INVALID_PERMISSIONS

- [ ] **403 Forbidden**
  - Description: "Forbidden - SUPER_ADMIN role required"
  - No schema (simple message)

- [ ] **404 Not Found**
  - Description: "Tenant not found"
  - Schema: `ErrorResponse`
  - Error code: "TENANT_NOT_FOUND"

- [ ] **409 Conflict**
  - Description: "Email already exists"
  - Schema: `ErrorResponse`
  - Error code: "EMAIL_ALREADY_EXISTS"

**500 Range Responses:**
- [ ] **500 Internal Server Error** (implicit, may be auto-documented)
  - Schema: `ErrorResponse`
  - Possible errors: USER_CREATION_FAILED, ADMIN_CREATION_FAILED

#### 3.5 Response Body Schema (TenantAdminResponse)
- [ ] Schema name: `TenantAdminResponse`
- [ ] Description: "Response payload containing created tenant administrator information"

**Response Fields:**
- [ ] `userId` (string)
  - Description: "User ID"
  - Example: "123e4567-e89b-12d3-a456-426614174000"
- [ ] `email` (string)
  - Description: "User email"
  - Example: "admin@tenant.com"
- [ ] `name` (string)
  - Description: "User name"
  - Example: "Tenant Administrator"
- [ ] `role` (string)
  - Description: "User role (always ADMIN)"
  - Example: "ADMIN"
- [ ] `tenantId` (string)
  - Description: "Associated tenant ID"
  - Example: "123e4567-e89b-12d3-a456-426614174000"
- [ ] `adminId` (string)
  - Description: "Admin ID (same as user ID)"
  - Example: "123e4567-e89b-12d3-a456-426614174000"
- [ ] `permissions` (array of Permission)
  - Description: "Admin permissions"
- [ ] `createdAt` (string)
  - Description: "Creation timestamp (ISO 8601)"
  - Example: "2024-02-26T10:30:00Z"

---

## Code Implementation Verification

### OpenAPI Annotations Present in Code

The following annotations are present in `/home/joao/workspace/k12/back/src/main/java/com/k12/tenant/infrastructure/rest/resource/TenantResource.java`:

```java
@POST
@Path("/{id}/admins")
@Operation(
    summary = "Create a tenant administrator",
    description = "Creates a new ADMIN user and Admin specialization for a specific tenant"
)
@APIResponses({
    @APIResponse(
        responseCode = "201",
        description = "Admin created successfully",
        content = @Content(schema = @Schema(implementation = TenantAdminResponse.class))
    ),
    @APIResponse(
        responseCode = "400",
        description = "Invalid request data",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @APIResponse(responseCode = "403", description = "Forbidden - SUPER_ADMIN role required"),
    @APIResponse(
        responseCode = "404",
        description = "Tenant not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @APIResponse(
        responseCode = "409",
        description = "Email already exists",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
})
public Response createTenantAdmin(
    @Parameter(description = "Tenant ID", required = true,
               example = "123e4567-e89b-12d3-a456-426614174000")
    @PathParam("id") String id,
    @Valid CreateTenantAdminRequest request
)
```

### DTO Schema Annotations

**CreateTenantAdminRequest** (lines 11-23):
- [x] `@Schema(description = "Request payload for creating a tenant administrator")`
- [x] All fields have appropriate `@Schema` annotations with descriptions, examples, and required flags
- [x] Email field has format validation
- [x] Password field has min length validation
- [x] Permissions field has required validation

**TenantAdminResponse** (lines 11-34):
- [x] `@Schema(description = "Response payload containing created tenant administrator information")`
- [x] All fields have appropriate `@Schema` annotations with descriptions and examples
- [x] Timestamp field includes ISO 8601 format in description

---

## Known Issues and Limitations

### Current Compilation Issues
As of 2026-02-26, there are compilation errors in other files that prevent the application from starting:

1. **PasswordHasher.java** - has syntax errors
2. **AdminFactory.java** - has compilation issues
3. **TenantAdminError.java** - has modifications that may not compile

**Impact:** Cannot perform live testing of Swagger UI until these are resolved.

### Alternative Verification Methods

#### Method 1: OpenAPI JSON Direct Access
Once the application compiles and starts:
```bash
curl http://localhost:8080/q/openapi | jq '.paths."/api/tenants/{id}/admins"'
```

#### Method 2: OpenAPI YAML Export
Quarkus also supports YAML format:
```bash
curl -H "Accept: application/yaml" http://localhost:8080/q/openapi
```

#### Method 3: IDE Integration
- If using IntelliJ IDEA with Quarkus tools:
  - Open the OpenAPI view
  - Search for the endpoint
  - Verify annotations are picked up correctly

---

## Expected OpenAPI JSON Structure

Below is the expected structure that should be generated (key sections):

```json
{
  "paths": {
    "/api/tenants/{id}/admins": {
      "post": {
        "tags": ["Tenants"],
        "summary": "Create a tenant administrator",
        "description": "Creates a new ADMIN user and Admin specialization for a specific tenant",
        "operationId": "createTenantAdmin",
        "parameters": [{
          "name": "id",
          "in": "path",
          "description": "Tenant ID",
          "required": true,
          "schema": {
            "type": "string",
            "format": "uuid",
            "example": "123e4567-e89b-12d3-a456-426614174000"
          }
        }],
        "requestBody": {
          "description": "Tenant admin creation request",
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/CreateTenantAdminRequest"
              }
            }
          }
        },
        "responses": {
          "201": {
            "description": "Admin created successfully",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/TenantAdminResponse"
                }
              }
            }
          },
          "400": {
            "description": "Invalid request data",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          },
          "403": {
            "description": "Forbidden - SUPER_ADMIN role required"
          },
          "404": {
            "description": "Tenant not found",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          },
          "409": {
            "description": "Email already exists",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          }
        },
        "security": [{
          "SecurityScheme": []
        }]
      }
    }
  },
  "components": {
    "schemas": {
      "CreateTenantAdminRequest": {
        "type": "object",
        "required": ["email", "password", "name", "permissions"],
        "properties": {
          "email": {
            "type": "string",
            "format": "email",
            "description": "Admin email address",
            "example": "admin@tenant.com"
          },
          "password": {
            "type": "string",
            "minLength": 8,
            "description": "Admin password (will be hashed)",
            "example": "SecurePass123"
          },
          "name": {
            "type": "string",
            "description": "Admin full name",
            "example": "Tenant Administrator"
          },
          "permissions": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/Permission"
            },
            "description": "Set of permissions for this admin"
          }
        }
      },
      "TenantAdminResponse": {
        "type": "object",
        "properties": {
          "userId": {
            "type": "string",
            "format": "uuid",
            "description": "User ID"
          },
          "email": {
            "type": "string",
            "format": "email",
            "description": "User email"
          },
          "name": {
            "type": "string",
            "description": "User name"
          },
          "role": {
            "type": "string",
            "enum": ["ADMIN"],
            "description": "User role (always ADMIN)"
          },
          "tenantId": {
            "type": "string",
            "format": "uuid",
            "description": "Associated tenant ID"
          },
          "adminId": {
            "type": "string",
            "format": "uuid",
            "description": "Admin ID (same as user ID)"
          },
          "permissions": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/Permission"
            },
            "description": "Admin permissions"
          },
          "createdAt": {
            "type": "string",
            "format": "date-time",
            "description": "Creation timestamp (ISO 8601)"
          }
        }
      }
    }
  }
}
```

---

## Verification Status

### Static Code Analysis: ✅ COMPLETE
- [x] All OpenAPI annotations present in TenantResource
- [x] Request DTO has complete schema annotations
- [x] Response DTO has complete schema annotations
- [x] All response codes documented
- [x] Security requirements documented (@RolesAllowed("SUPER_ADMIN"))
- [x] Parameter documentation complete

### Live Testing: ⏸️ BLOCKED
- [ ] Application compilation issues prevent live testing
- [ ] Cannot access Swagger UI
- [ ] Cannot verify actual OpenAPI JSON output
- [ ] Cannot perform interactive testing through Swagger UI

### Recommendations
1. **Fix compilation errors** in other files first
2. **Start application** and access Swagger UI at `/q/swagger-ui`
3. **Perform interactive testing** using the "Try it out" button
4. **Export OpenAPI spec** for documentation generation
5. **Compare** actual OpenAPI output with expected structure above

---

## Additional Resources

### Quarkus OpenAPI Documentation
- [Quarkus OpenAPI Guide](https://quarkus.io/guides/openapi-swaggerui)
- [MicroProfile OpenAPI Specification](https://github.com/eclipse/microprofile-open-api)

### Related Files
- **Resource:** `/home/joao/workspace/k12/back/src/main/java/com/k12/tenant/infrastructure/rest/resource/TenantResource.java`
- **Request DTO:** `/home/joao/workspace/k12/back/src/main/java/com/k12/tenant/infrastructure/rest/dto/CreateTenantAdminRequest.java`
- **Response DTO:** `/home/joao/workspace/k12/back/src/main/java/com/k12/tenant/infrastructure/rest/dto/TenantAdminResponse.java`
- **Error Response:** `/home/joao/workspace/k12/back/src/main/java/com/k12/tenant/infrastructure/rest/dto/ErrorResponse.java`

---

## Next Steps

1. Resolve compilation errors in:
   - `PasswordHasher.java`
   - `AdminFactory.java`
   - `TenantAdminError.java`

2. Once compilation succeeds, perform live verification using this checklist

3. Document any discrepancies between expected and actual OpenAPI output

4. Update this checklist with actual verification results

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Status:** Static verification complete, live testing pending
