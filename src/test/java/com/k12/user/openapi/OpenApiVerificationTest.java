package com.k12.user.openapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OpenApiVerificationTest {

    @Test
    @DisplayName("Should have OpenAPI endpoint accessible")
    void openApiEndpointAccessible() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("openapi", notNullValue())
                .body("info.title", equalTo("K12 Backend API"));
    }

    @Test
    @DisplayName("Should have login endpoint documented")
    void loginEndpointDocumented() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.\"/api/auth/login\"", notNullValue())
                .body("paths.\"/api/auth/login\".post", notNullValue())
                .body("paths.\"/api/auth/login\".post.summary", equalTo("Authenticate user"))
                .body(
                        "paths.\"/api/auth/login\".post.description",
                        equalTo("Authenticates a user with email and password, returns JWT token"));
    }

    @Test
    @DisplayName("Should have request body schema for login")
    void loginRequestBodyDocumented() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.\"/api/auth/login\".post.requestBody", notNullValue())
                .body("paths.\"/api/auth/login\".post.requestBody.description", equalTo("Login credentials"))
                .body("paths.\"/api/auth/login\".post.requestBody.required", equalTo(true))
                .body("paths.\"/api/auth/login\".post.requestBody.content.\"application/json\"", notNullValue());
    }

    @Test
    @DisplayName("Should have 200 response documented")
    void login200ResponseDocumented() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.\"/api/auth/login\".post.responses.\"200\"", notNullValue())
                .body(
                        "paths.\"/api/auth/login\".post.responses.\"200\".description",
                        equalTo("Authentication successful"))
                .body(
                        "paths.\"/api/auth/login\".post.responses.\"200\".content.\"application/json\".schema.$ref",
                        containsString("LoginResponseDTO"));
    }

    @Test
    @DisplayName("Should have 401 response documented")
    void login401ResponseDocumented() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.\"/api/auth/login\".post.responses.\"401\"", notNullValue())
                .body("paths.\"/api/auth/login\".post.responses.\"401\".description", equalTo("Invalid credentials"))
                .body(
                        "paths.\"/api/auth/login\".post.responses.\"401\".content.\"application/json\".schema.$ref",
                        containsString("ErrorResponseDTO"));
    }

    @Test
    @DisplayName("Should have 400 response documented")
    void login400ResponseDocumented() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.\"/api/auth/login\".post.responses.\"400\"", notNullValue())
                .body("paths.\"/api/auth/login\".post.responses.\"400\".description", equalTo("Invalid request"))
                .body(
                        "paths.\"/api/auth/login\".post.responses.\"400\".content.\"application/json\".schema.$ref",
                        containsString("ErrorResponseDTO"));
    }

    @Test
    @DisplayName("Should have LoginRequestDTO schema in components")
    void loginRequestDTOSchemaDefined() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas.LoginRequestDTO", notNullValue())
                .body("components.schemas.LoginRequestDTO.type", equalTo("object"))
                .body("components.schemas.LoginRequestDTO.required", hasItems("email", "password"))
                .body("components.schemas.LoginRequestDTO.properties.email", notNullValue())
                .body("components.schemas.LoginRequestDTO.properties.password", notNullValue());
    }

    @Test
    @DisplayName("Should have LoginResponseDTO schema in components")
    void loginResponseDTOSchemaDefined() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas.LoginResponseDTO", notNullValue())
                .body("components.schemas.LoginResponseDTO.type", equalTo("object"))
                .body("components.schemas.LoginResponseDTO.properties.token", notNullValue())
                .body("components.schemas.LoginResponseDTO.properties.user", notNullValue());
    }

    @Test
    @DisplayName("Should have ErrorResponseDTO schema in components")
    void errorResponseDTOSchemaDefined() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.schemas.ErrorResponseDTO", notNullValue())
                .body("components.schemas.ErrorResponseDTO.type", equalTo("object"))
                .body("components.schemas.ErrorResponseDTO.properties.error", notNullValue())
                .body("components.schemas.ErrorResponseDTO.properties.message", notNullValue())
                .body("components.schemas.ErrorResponseDTO.properties.timestamp", notNullValue());
    }

    @Test
    @DisplayName("Should have Authentication tag")
    void authenticationTagPresent() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("tags", hasItem(hasEntry("name", "Authentication")))
                .body(
                        "tags.findAll { it.name == 'Authentication' }[0].description",
                        equalTo("User authentication operations"));
    }

    @Test
    @DisplayName("Should have server URL configured")
    void serverUrlConfigured() {
        given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("servers", notNullValue())
                .body("servers[0].url", equalTo("http://localhost:8080"));
    }
}
