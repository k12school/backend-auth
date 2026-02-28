package com.k12.tenant.infrastructure.rest.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.k12.backend.infrastructure.jooq.public_.tables.Tenants;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.tenant.domain.port.TenantRepository;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.infrastructure.security.TokenService;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantResourceCreateAdminIntegrationTest {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    TenantRepository tenantRepository;

    private static String SUPER_ADMIN_TOKEN;

    @BeforeAll
    static void setUp() {
        // Create a SUPER_ADMIN user for testing
        var testUser = new com.k12.user.domain.models.User(
                new UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                new EmailAddress("admin@k12.com"),
                new PasswordHash("$2b$12$Rc0dj3Wk0R6QhLlAb9Ocoeq5u1DcbIOrjlx3dY93vtJwJ1UgVE7aS"),
                Set.of(UserRole.SUPER_ADMIN),
                UserStatus.ACTIVE,
                new UserName("Super Administrator"),
                new TenantId("00000000-0000-0000-0000-000000000001"));

        // Generate a real JWT token
        SUPER_ADMIN_TOKEN = TokenService.generateToken(testUser, "00000000-0000-0000-0000-000000000001");
    }

    @BeforeEach
    @Transactional
    void ensureTenantExists() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        // Check if tenant exists in the database
        Long existingCount = ctx.selectCount()
                .from(Tenants.TENANTS)
                .where(Tenants.TENANTS.ID.eq(UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .fetchOne(0, Long.class);

        if (existingCount == null || existingCount == 0) {
            // Use the repository to create tenant with proper event sourcing
            var tenantId = new TenantId("00000000-0000-0000-0000-000000000001");
            var tenantName =
                    com.k12.tenant.domain.models.TenantName.of("Default Tenant").get();
            var subdomain = com.k12.tenant.domain.models.Subdomain.of("default").get();

            var createdEvent = new com.k12.tenant.domain.models.events.TenantEvents.TenantCreated(
                    tenantId,
                    tenantName,
                    subdomain,
                    com.k12.tenant.domain.models.TenantStatus.ACTIVE,
                    Instant.now(),
                    1L);

            // Append the event (this will handle both event store and projection)
            tenantRepository.append(createdEvent, -1);
        }
    }

    @Test
    void createTenantAdmin_withSuperAdminToken_returns201() {
        String tenantId = "00000000-0000-0000-0000-000000000001"; // From Flyway migration

        given().auth()
                .oauth2(SUPER_ADMIN_TOKEN)
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
        String tenantId = "00000000-0000-0000-0000-000000000001"; // From Flyway migration

        given().auth()
                .oauth2(SUPER_ADMIN_TOKEN)
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
        String tenantId = "00000000-0000-0000-0000-000000000001"; // From Flyway migration

        given().auth()
                .oauth2(SUPER_ADMIN_TOKEN)
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
        String tenantId = "00000000-0000-0000-0000-000000000001"; // From Flyway migration

        given().auth()
                .oauth2(SUPER_ADMIN_TOKEN)
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

        given().auth()
                .oauth2(SUPER_ADMIN_TOKEN)
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
