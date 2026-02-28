package com.k12.user.infrastructure.rest.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.k12.backend.infrastructure.jooq.public_.tables.Users;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;
import com.k12.user.infrastructure.security.TokenService;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserResourceTest {

    @Inject
    AgroalDataSource dataSource;

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
    void cleanDatabase() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        // Clean up test users
        ctx.deleteFrom(Users.USERS)
                .where(Users.USERS.EMAIL.like("test%@example.com"))
                .execute();
    }

    @Test
    void shouldCreateTeacherSuccessfully() {
        CreateUserRequest request = new CreateUserRequest(
                "test-teacher@example.com",
                "Password123!",
                "Test Teacher",
                new CreateUserRequest.UserRole("TEACHER"),
                new CreateUserRequest.TeacherData("EMP001", "Science", "2024-01-15"),
                null,
                null);

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(request)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(201, response.statusCode());
        assertNotNull(response.jsonPath().getString("id"));
        assertEquals("test-teacher@example.com", response.jsonPath().getString("email"));
        assertEquals("TEACHER", response.jsonPath().getString("roles[0]"));
        assertEquals("ACTIVE", response.jsonPath().getString("status"));
    }

    @Test
    void shouldCreateStudentSuccessfully() {
        CreateUserRequest request = new CreateUserRequest(
                "test-student@example.com",
                "Password123!",
                "Test Student",
                new CreateUserRequest.UserRole("STUDENT"),
                null,
                null,
                new CreateUserRequest.StudentData("STU001", "10th", "2010-05-15", null));

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(request)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(201, response.statusCode());
        assertNotNull(response.jsonPath().getString("id"));
        assertEquals("test-student@example.com", response.jsonPath().getString("email"));
        assertEquals("STUDENT", response.jsonPath().getString("roles[0]"));
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() {
        // Create first user
        CreateUserRequest request1 = new CreateUserRequest(
                "duplicate@example.com",
                "Password123!",
                "First User",
                new CreateUserRequest.UserRole("TEACHER"),
                new CreateUserRequest.TeacherData("EMP001", "Science", "2024-01-15"),
                null,
                null);

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(request1)
                .when()
                .post("/api/users")
                .then()
                .statusCode(201);

        // Try to create second user with same email
        CreateUserRequest request2 = new CreateUserRequest(
                "duplicate@example.com",
                "Password456!",
                "Second User",
                new CreateUserRequest.UserRole("PARENT"),
                null,
                new CreateUserRequest.ParentData("555-1234", "123 Main St", "Jane Doe"),
                null);

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(request2)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(409, response.statusCode());
        ErrorResponseDTO error = response.as(ErrorResponseDTO.class);
        assertEquals("CONFLICT_ERROR", error.error());
        assertTrue(error.message().contains("already exists"));
    }

    @Test
    void shouldReturn400OnInvalidEmail() {
        String invalidRequest = """
                {
                    "email": "invalid-email",
                    "password": "Password123!",
                    "name": "Test User",
                    "role": {"value": "TEACHER"},
                    "teacherData": {
                        "employeeId": "EMP001",
                        "department": "Science",
                        "hireDate": "2024-01-15"
                    }
                }
                """;

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(invalidRequest)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(400, response.statusCode());
    }

    @Test
    void shouldReturn400OnMissingRequiredFields() {
        String invalidRequest = """
                {
                    "email": "test@example.com",
                    "password": "short"
                }
                """;

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(invalidRequest)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(400, response.statusCode());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() {
        CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "Password123!",
                "Test User",
                new CreateUserRequest.UserRole("TEACHER"),
                new CreateUserRequest.TeacherData("EMP001", "Science", "2024-01-15"),
                null,
                null);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/users")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldCreateParentSuccessfully() {
        CreateUserRequest request = new CreateUserRequest(
                "test-parent@example.com",
                "Password123!",
                "Test Parent",
                new CreateUserRequest.UserRole("PARENT"),
                null,
                new CreateUserRequest.ParentData("555-1234", "123 Main St", "Jane Doe"),
                null);

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(request)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(201, response.statusCode());
        assertEquals("test-parent@example.com", response.jsonPath().getString("email"));
        assertEquals("PARENT", response.jsonPath().getString("roles[0]"));
    }

    @Test
    void shouldReturn400OnInvalidRole() {
        String invalidRequest = """
                {
                    "email": "test@example.com",
                    "password": "Password123!",
                    "name": "Test User",
                    "role": {"value": "INVALID_ROLE"}
                }
                """;

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(invalidRequest)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(400, response.statusCode());
    }

    @Test
    void shouldReturn400OnMissingSpecializationDataForTeacher() {
        String invalidRequest = """
                {
                    "email": "test@example.com",
                    "password": "Password123!",
                    "name": "Test User",
                    "role": {"value": "TEACHER"}
                }
                """;

        Response response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SUPER_ADMIN_TOKEN)
                .body(invalidRequest)
                .when()
                .post("/api/users")
                .then()
                .extract()
                .response();

        assertEquals(400, response.statusCode());
        ErrorResponseDTO error = response.as(ErrorResponseDTO.class);
        assertTrue(error.message().contains("teacherData") || error.message().contains("required"));
    }
}
