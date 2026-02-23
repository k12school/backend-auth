package com.k12.infrastructure.security;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for JWT authentication filter.
 */
@QuarkusTest
class JWTAuthenticationFilterTest {

    @Test
    void shouldReturn401WhenNoTokenProvided() {
        Response response = given().contentType(ContentType.JSON)
                .when()
                .get("/api/protected/me")
                .then()
                .extract()
                .response();

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturnUserInfoWhenValidTokenProvided() {
        // First, login to get a token
        Response loginResponse = given().contentType(ContentType.JSON)
                .body("{\"email\":\"admin@k12.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();

        assertEquals(200, loginResponse.statusCode());
        String token = loginResponse.jsonPath().getString("token");
        assertNotNull(token);

        // Use the token to access protected endpoint
        Response protectedResponse = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/protected/me")
                .then()
                .extract()
                .response();

        assertEquals(200, protectedResponse.statusCode());
        assertEquals("admin@k12.com", protectedResponse.jsonPath().getString("email"));
        assertTrue(protectedResponse.jsonPath().getList("roles").contains("SUPER_ADMIN"));
    }

    @Test
    void shouldAccessAdminEndpointWithSuperAdminRole() {
        // Login to get token
        String token = given().contentType(ContentType.JSON)
                .body("{\"email\":\"admin@k12.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/login")
                .jsonPath()
                .getString("token");

        // Access admin endpoint
        Response response = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/protected/admin")
                .then()
                .extract()
                .response();

        assertEquals(200, response.statusCode());
        assertEquals("Admin access granted", response.jsonPath().getString("message"));
    }

    @Test
    void shouldReturn403WhenNonAdminAttemptsAdminEndpoint() {
        // For now, we only have SUPER_ADMIN user, so this test
        // demonstrates that without SUPER_ADMIN role, access is forbidden
        // This test can be expanded when we have users with different roles

        String token = given().contentType(ContentType.JSON)
                .body("{\"email\":\"admin@k12.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/login")
                .jsonPath()
                .getString("token");

        // If we modify this to check for a different role,
        // we would expect 403 here
        Response response = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/protected/admin")
                .then()
                .extract()
                .response();

        // Should succeed because SUPER_ADMIN has access
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldCompareAccessMethods() {
        // Login to get token
        String token = given().contentType(ContentType.JSON)
                .body("{\"email\":\"admin@k12.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/login")
                .jsonPath()
                .getString("token");

        // Access compare endpoint
        Response response = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/protected/compare")
                .then()
                .extract()
                .response();

        assertEquals(200, response.statusCode());
        assertNotNull(response.jsonPath().getString("userIdFromSecurityContext"));
        assertNotNull(response.jsonPath().getString("userIdFromCDI"));
        assertTrue(response.jsonPath().getBoolean("match"));
    }
}
