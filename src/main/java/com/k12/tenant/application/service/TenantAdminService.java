package com.k12.tenant.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.tenant.domain.models.error.TenantAdminError;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserFactory;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.events.UserEvents;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.AdminFactory;
import com.k12.user.domain.models.specialization.admin.AdminId;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.security.PasswordHasher;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application service for creating tenant administrators.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TenantAdminService {

    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final Tracer tracer;
    private static final Logger log = LoggerFactory.getLogger(TenantAdminService.class);

    /**
     * Creates a new tenant administrator for the specified tenant.
     *
     * @param tenantId the tenant ID to associate the admin with
     * @param request the create tenant admin request
     * @return Result containing TenantAdminResponse on success, or TenantAdminError on failure
     */
    @Transactional
    public Result<TenantAdminResponse, TenantAdminError> createTenantAdmin(
            TenantId tenantId, CreateTenantAdminRequest request) {
        log.info("Creating tenant admin for tenant: {}", tenantId.value());

        Span span = tracer.spanBuilder("TenantAdminService.createTenantAdmin")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            // Step 1: Validate tenant exists
            var tenantResult = tenantService.getTenant(tenantId);
            if (tenantResult.isFailure()) {
                log.warn("Tenant not found: {}", tenantId.value());
                span.setStatus(StatusCode.ERROR, "Tenant not found");
                return Result.failure(TenantAdminError.ConflictError.TENANT_NOT_FOUND);
            }

            // Step 2: Check if email already exists
            Optional<User> existingUser = userRepository.findByEmailAddress(request.email());
            if (existingUser.isPresent()) {
                log.warn("Email already exists: {}", request.email());
                span.setStatus(StatusCode.ERROR, "Email already exists");
                return Result.failure(TenantAdminError.ConflictError.EMAIL_ALREADY_EXISTS);
            }

            // Step 3: Validate and create value objects
            EmailAddress emailAddress;
            try {
                emailAddress = EmailAddress.of(request.email());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid email format: {}", request.email());
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "Invalid email format");
                return Result.failure(TenantAdminError.ValidationError.INVALID_EMAIL);
            }

            UserName userName;
            try {
                userName = UserName.of(request.name());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid name: {}", request.name());
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "Invalid name");
                return Result.failure(TenantAdminError.ValidationError.INVALID_NAME);
            }

            // Step 4: Hash password
            PasswordHash passwordHash;
            try {
                passwordHash = PasswordHasher.hash(request.password());
            } catch (IllegalArgumentException e) {
                log.error("Password hashing failed", e);
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "Password hashing failed");
                return Result.failure(TenantAdminError.ValidationError.INVALID_PASSWORD);
            }

            // Step 5: Create User with ADMIN role
            Result<UserEvents, UserError> userResult =
                    UserFactory.create(emailAddress, passwordHash, Set.of(UserRole.ADMIN), userName);

            if (userResult.isFailure()) {
                log.error("User creation failed");
                span.setStatus(StatusCode.ERROR, "User creation failed");
                return Result.failure(TenantAdminError.PersistenceError.USER_CREATION_FAILED);
            }

            UserEvents.UserCreated userCreated = (UserEvents.UserCreated) userResult.get();
            UserId userId = userCreated.userId();

            // Step 6: Save User with tenant association
            User user = new User(
                    userId, emailAddress, passwordHash, Set.of(UserRole.ADMIN), userCreated.status(), userName);
            userRepository.save(user);

            // Step 7: Create Admin aggregate
            AdminId adminId = AdminId.of(userId);
            Admin admin;
            try {
                admin = AdminFactory.create(adminId, request.permissions());
            } catch (IllegalArgumentException e) {
                log.error("Admin creation failed: {}", e.getMessage());
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "Admin creation failed");
                return Result.failure(TenantAdminError.ValidationError.INVALID_PERMISSIONS);
            }

            // Step 8: Save Admin
            try {
                adminRepository.save(admin);
            } catch (RuntimeException e) {
                log.error("Failed to save admin aggregate", e);
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "Failed to save admin");
                return Result.failure(TenantAdminError.PersistenceError.ADMIN_CREATION_FAILED);
            }

            // Step 9: Build response
            TenantAdminResponse response = TenantAdminResponse.from(user, admin, tenantId);
            log.info("Successfully created tenant admin: {} for tenant: {}", userId.value(), tenantId.value());

            span.setStatus(StatusCode.OK);
            return Result.success(response);
        } catch (Exception e) {
            log.error("Unexpected error creating tenant admin", e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
