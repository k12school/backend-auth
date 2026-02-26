package com.k12.tenant.infrastructure.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CreateTenantAdminRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequest_passesValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
                "admin@tenant.com",
                "SecurePass123",
                "Tenant Admin",
                Set.of(Permission.USER_MANAGEMENT, Permission.COURSE_MANAGEMENT));

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void blankEmail_failsValidation() {
        CreateTenantAdminRequest request =
                new CreateTenantAdminRequest("", "SecurePass123", "Tenant Admin", Set.of(Permission.USER_MANAGEMENT));

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size(), "Should have violations for blank and invalid email");
    }

    @Test
    void invalidEmailFormat_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
                "not-an-email", "SecurePass123", "Tenant Admin", Set.of(Permission.USER_MANAGEMENT));

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("Email must be valid", violations.iterator().next().getMessage());
    }

    @Test
    void shortPassword_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
                "admin@tenant.com", "short", "Tenant Admin", Set.of(Permission.USER_MANAGEMENT));

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals(
                "Password must be at least 8 characters",
                violations.iterator().next().getMessage());
    }

    @Test
    void blankName_failsValidation() {
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
                "admin@tenant.com", "SecurePass123", " ", Set.of(Permission.USER_MANAGEMENT));

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("Name is required", violations.iterator().next().getMessage());
    }

    @Test
    void emptyPermissions_failsValidation() {
        CreateTenantAdminRequest request =
                new CreateTenantAdminRequest("admin@tenant.com", "SecurePass123", "Tenant Admin", Set.of());

        Set<ConstraintViolation<CreateTenantAdminRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("permission"));
    }
}
