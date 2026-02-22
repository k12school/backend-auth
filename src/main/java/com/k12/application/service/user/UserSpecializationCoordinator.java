package com.k12.application.service.user;

import com.k12.domain.model.common.Result;
import com.k12.domain.model.common.UserId;
import com.k12.domain.model.user.UserRole;
import com.k12.domain.model.user.error.UserError;
import com.k12.domain.ports.out.AdminRepository;
import com.k12.domain.ports.out.ParentRepository;
import com.k12.domain.ports.out.StudentRepository;
import com.k12.domain.ports.out.TeacherRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Coordinates user specializations with user roles.
 * Ensures that specialization profiles exist when roles are added,
 * and handles cleanup when roles are removed.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserSpecializationCoordinator {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final AdminRepository adminRepository;

    /**
     * Ensures a specialization profile exists when a role is added to a user.
     * Called after UserRoleAdded event.
     *
     * @param userId The user ID
     * @param addedRole The role that was added
     * @return Result containing Void on success, or UserError on failure
     */
    public Result<Void, com.k12.domain.model.user.error.UserError> ensureSpecializationExists(
            UserId userId, UserRole addedRole) {
        log.info("Ensuring specialization exists for userId: {} with role: {}", userId, addedRole);

        return switch (addedRole) {
            case TEACHER -> ensureTeacherProfileExists(userId);
            case STUDENT -> ensureStudentProfileExists(userId);
            case PARENT -> ensureParentProfileExists(userId);
            case ADMIN, SUPER_ADMIN -> ensureAdminProfileExists(userId);
            default -> Result.success(null);
        };
    }

    /**
     * Handles role removal by archiving or removing the specialization profile.
     * Called after UserRoleRemoved event.
     *
     * @param userId The user ID
     * @param removedRole The role that was removed
     * @return Result containing Void on success, or UserError on failure
     */
    public Result<Void, UserError> handleRoleRemoval(UserId userId, UserRole removedRole) {
        log.info("Handling role removal for userId: {} with role: {}", userId, removedRole);

        return switch (removedRole) {
            case TEACHER -> archiveTeacherProfile(userId);
            case STUDENT -> archiveStudentProfile(userId);
            case PARENT -> archiveParentProfile(userId);
            case ADMIN, SUPER_ADMIN -> archiveAdminProfile(userId);
            default -> Result.success(null);
        };
    }

    /**
     * Ensures a teacher profile exists for the user.
     */
    private Result<Void, UserError> ensureTeacherProfileExists(UserId userId) {
        if (teacherRepository.existsByUserId(userId)) {
            log.debug("Teacher profile already exists for userId: {}", userId);
            return Result.success(null);
        }

        log.info("Creating default teacher profile for userId: {}", userId);

        // TODO: Create default teacher profile
        // This would require default values for department, qualifications, subjects
        // For now, we just log that this needs to be handled
        log.warn("Teacher profile creation not implemented - requires business rules for defaults");

        return Result.success(null);
    }

    /**
     * Ensures a student profile exists for the user.
     */
    private Result<Void, UserError> ensureStudentProfileExists(UserId userId) {
        if (studentRepository.existsByUserId(userId)) {
            log.debug("Student profile already exists for userId: {}", userId);
            return Result.success(null);
        }

        log.info("Creating default student profile for userId: {}", userId);

        // TODO: Create default student profile
        // This would require default values for grade level and guardian info
        log.warn("Student profile creation not implemented - requires business rules for defaults");

        return Result.success(null);
    }

    /**
     * Ensures a parent profile exists for the user.
     */
    private Result<Void, UserError> ensureParentProfileExists(UserId userId) {
        if (parentRepository.existsByUserId(userId)) {
            log.debug("Parent profile already exists for userId: {}", userId);
            return Result.success(null);
        }

        log.info("Creating default parent profile for userId: {}", userId);

        // TODO: Create default parent profile
        // This would require default values for contact preference and relationships
        log.warn("Parent profile creation not implemented - requires business rules for defaults");

        return Result.success(null);
    }

    /**
     * Ensures an admin profile exists for the user.
     */
    private Result<Void, UserError> ensureAdminProfileExists(UserId userId) {
        if (adminRepository.existsByUserId(userId)) {
            log.debug("Admin profile already exists for userId: {}", userId);
            return Result.success(null);
        }

        log.info("Creating default admin profile for userId: {}", userId);

        // TODO: Create default admin profile
        // This would require default values for permission level and scope
        log.warn("Admin profile creation not implemented - requires business rules for defaults");

        return Result.success(null);
    }

    /**
     * Archives or removes a teacher profile when the TEACHER role is removed.
     */
    private Result<Void, UserError> archiveTeacherProfile(UserId userId) {
        log.info("Archiving teacher profile for userId: {}", userId);

        // TODO: Implement archiving logic
        // Options:
        // 1. Soft delete (set status to ARCHIVED)
        // 2. Hard delete (remove from database)
        // 3. Keep for audit but mark as inactive

        return Result.success(null);
    }

    /**
     * Archives or removes a student profile when the STUDENT role is removed.
     */
    private Result<Void, UserError> archiveStudentProfile(UserId userId) {
        log.info("Archiving student profile for userId: {}", userId);

        // TODO: Implement archiving logic
        // For students, we might want to keep the record for historical purposes

        return Result.success(null);
    }

    /**
     * Archives or removes a parent profile when the PARENT role is removed.
     */
    private Result<Void, UserError> archiveParentProfile(UserId userId) {
        log.info("Archiving parent profile for userId: {}", userId);

        // TODO: Implement archiving logic

        return Result.success(null);
    }

    /**
     * Archives or removes an admin profile when the ADMIN/SUPER_ADMIN role is removed.
     */
    private Result<Void, UserError> archiveAdminProfile(UserId userId) {
        log.info("Archiving admin profile for userId: {}", userId);

        // TODO: Implement archiving logic
        // For admins, we should keep audit trail but revoke access

        return Result.success(null);
    }

    /**
     * Validates that all required specialization profiles exist for a user's roles.
     *
     * @param userId The user ID
     * @param roles The user's roles
     * @return Result containing Void on success, or UserError on failure
     */
    public Result<Void, UserError> validateSpecializations(UserId userId, java.util.Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Result.success(null);
        }

        for (UserRole role : roles) {
            switch (role) {
                case TEACHER -> {
                    if (!teacherRepository.existsByUserId(userId)) {
                        return Result.failure(UserError.ValidationError.VALUE_REQUIRED);
                    }
                }
                case STUDENT -> {
                    if (!studentRepository.existsByUserId(userId)) {
                        return Result.failure(UserError.ValidationError.VALUE_REQUIRED);
                    }
                }
                case PARENT -> {
                    if (!parentRepository.existsByUserId(userId)) {
                        return Result.failure(UserError.ValidationError.VALUE_REQUIRED);
                    }
                }
                case ADMIN, SUPER_ADMIN -> {
                    if (!adminRepository.existsByUserId(userId)) {
                        return Result.failure(UserError.ValidationError.VALUE_REQUIRED);
                    }
                }
            }
        }

        return Result.success(null);
    }
}
