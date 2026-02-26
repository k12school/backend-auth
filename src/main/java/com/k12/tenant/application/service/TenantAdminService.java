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
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.events.UserEvents;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.AdminFactory;
import com.k12.user.domain.models.specialization.admin.AdminId;
import com.k12.user.domain.models.UserFactory;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.security.PasswordHasher;
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

        // Step 1: Validate tenant exists
        var tenantResult = tenantService.getTenant(tenantId);
        if (tenantResult.isError()) {
            log.warn("Tenant not found: {}", tenantId.value());
            return Result.failure(TenantAdminError.TenantNotFoundError.TENANT_NOT_FOUND);
        }

        // Step 2: Check if email already exists
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            log.warn("Email already exists: {}", request.email());
            return Result.failure(TenantAdminError.ConflictError.EMAIL_ALREADY_EXISTS);
        }

        // Step 3: Validate and create value objects
        EmailAddress emailAddress;
        try {
            emailAddress = EmailAddress.of(request.email());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid email format: {}", request.email());
            return Result.failure(TenantAdminError.ValidationError.INVALID_EMAIL);
        }

        UserName userName;
        try {
            userName = UserName.of(request.name());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid name: {}", request.name());
            return Result.failure(TenantAdminError.ValidationError.INVALID_NAME);
        }

        // Step 4: Hash password
        PasswordHash passwordHash;
        try {
            passwordHash = PasswordHasher.hash(request.password());
        } catch (Exception e) {
            log.error("Password hashing failed", e);
            return Result.failure(TenantAdminError.ValidationError.INVALID_PASSWORD);
        }

        // Step 5: Create User with ADMIN role
        Result<UserEvents, ?> userResult = UserFactory.create(
                emailAddress,
                passwordHash,
                Set.of(UserRole.ADMIN), // HARDCODED AS ADMIN
                userName);

        if (userResult.isError()) {
            log.error("User creation failed");
            return Result.failure(TenantAdminError.PersistenceError.USER_CREATION_FAILED);
        }

        UserEvents.UserCreated userCreated = (UserEvents.UserCreated) userResult.getSuccess();
        UserId userId = userCreated.userId();

        // Step 6: Save User with tenant association
        User user =
                new User(userId, emailAddress, passwordHash, Set.of(UserRole.ADMIN), userCreated.status(), userName);
        userRepository.save(user);

        // Step 7: Create Admin aggregate
        AdminId adminId = AdminId.of(userId);
        Admin admin;
        try {
            admin = AdminFactory.create(adminId, request.permissions());
        } catch (IllegalArgumentException e) {
            log.error("Admin creation failed: {}", e.getMessage());
            return Result.failure(TenantAdminError.ValidationError.INVALID_PERMISSIONS);
        }

        // Step 8: Save Admin
        try {
            adminRepository.save(admin);
        } catch (Exception e) {
            log.error("Failed to save admin aggregate", e);
            return Result.failure(TenantAdminError.PersistenceError.ADMIN_CREATION_FAILED);
        }

        // Step 9: Build response
        TenantAdminResponse response = TenantAdminResponse.from(user, admin, tenantId);
        log.info("Successfully created tenant admin: {} for tenant: {}", userId.value(), tenantId.value());

        return Result.success(response);
    }
}
