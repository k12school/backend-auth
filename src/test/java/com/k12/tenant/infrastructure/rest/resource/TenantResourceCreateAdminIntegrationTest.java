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

    // TODO: Replace with actual token generation or use test authentication
    private static final String SUPER_ADMIN_TOKEN = "valid-super-admin-jwt";

    @Test
    void createTenantAdmin_withSuperAdminToken_returns201() {
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
                    "email": "admin@fake.com",
                    "password": "SecurePass123",
                    "name": "Fake Admin",
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
