package com.k12.application.service.authorization;

import com.k12.domain.model.authorization.Permission;
import com.k12.domain.model.common.UserId;
import com.k12.domain.model.user.User;
import com.k12.domain.model.user.UserRole;
import com.k12.domain.ports.out.AdminRepository;
import com.k12.domain.ports.out.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;

/**
 * Service for checking user permissions and authorization.
 * Coordinates between User roles and specialization-specific permissions.
 */
@ApplicationScoped
public class AuthorizationService {

    @Inject
    UserRepository userRepository;

    @Inject
    AdminRepository adminRepository;

    // TODO: Inject other repositories when they are implemented
    // @Inject
    // TeacherRepository teacherRepository;
    //
    // @Inject
    // StudentRepository studentRepository;
    //
    // @Inject
    // ParentRepository parentRepository;

    /**
     * Checks if a user has a specific permission.
     * Combines role-based permissions with any additional permissions from specializations.
     *
     * @param userId The user to check
     * @param required The permission required
     * @return true if the user has the permission
     */
    public boolean hasPermission(UserId userId, Permission required) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        Set<UserRole> roles = user.userRole();

        // Get base permissions from roles
        Set<Permission> permissions = RolePermissions.getPermissionsForRoles(roles);

        // Add any additional permissions from Admin profile if user is an admin
        if (roles.contains(UserRole.ADMIN) || roles.contains(UserRole.SUPER_ADMIN)) {
            adminRepository.findByUserId(userId).ifPresent(admin -> {
                permissions.addAll(convertAdminPermissions(admin.permissions()));
            });
        }

        // TODO: Add additional permissions from other specializations if needed
        // if (roles.contains(UserRole.TEACHER)) {
        //     teacherRepository.findByUserId(userId).ifPresent(teacher -> {
        //         // Add teacher-specific permissions
        //     });
        // }

        return permissions.contains(required);
    }

    /**
     * Checks if a user has all of the specified permissions.
     *
     * @param userId The user to check
     * @param required Set of permissions required
     * @return true if the user has all permissions
     */
    public boolean hasAllPermissions(UserId userId, Set<Permission> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }

        return required.stream().allMatch(permission -> hasPermission(userId, permission));
    }

    /**
     * Checks if a user has any of the specified permissions.
     *
     * @param userId The user to check
     * @param permissions Set of permissions to check
     * @return true if the user has at least one permission
     */
    public boolean hasAnyPermission(UserId userId, Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }

        return permissions.stream().anyMatch(permission -> hasPermission(userId, permission));
    }

    /**
     * Gets all permissions for a user.
     * Combines role-based permissions with specialization-specific permissions.
     *
     * @param userId The user to get permissions for
     * @return Set of all permissions the user has
     */
    public Set<Permission> getUserPermissions(UserId userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Set.of();
        }

        User user = userOpt.get();
        Set<UserRole> roles = user.userRole();

        // Get base permissions from roles
        Set<Permission> permissions = RolePermissions.getPermissionsForRoles(roles);

        // Add admin-specific permissions
        if (roles.contains(UserRole.ADMIN) || roles.contains(UserRole.SUPER_ADMIN)) {
            adminRepository.findByUserId(userId).ifPresent(admin -> {
                permissions.addAll(convertAdminPermissions(admin.permissions()));
            });
        }

        // TODO: Add permissions from other specializations

        return permissions;
    }

    /**
     * Checks if a user has a specific role.
     *
     * @param userId The user to check
     * @param role The role to check for
     * @return true if the user has the role
     */
    public boolean hasRole(UserId userId, UserRole role) {
        return userRepository
                .findById(userId)
                .map(user -> user.userRole().contains(role))
                .orElse(false);
    }

    /**
     * Converts Admin-specific permissions to domain Permission enum.
     * This bridges the gap between admin specialization and domain permissions.
     */
    private Set<Permission> convertAdminPermissions(
            Set<com.k12.domain.model.specialization.admin.valueobjects.Permission> adminPermissions) {
        return adminPermissions.stream()
                .map(this::convertAdminPermission)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Converts a single Admin permission to domain Permission.
     */
    private Optional<Permission> convertAdminPermission(
            com.k12.domain.model.specialization.admin.valueobjects.Permission adminPermission) {
        return switch (adminPermission) {
            case USER_MANAGEMENT -> Optional.of(Permission.MANAGE_USERS);
            case MANAGE_USERS -> Optional.of(Permission.MANAGE_USERS);
            case CREATE_USER -> Optional.of(Permission.CREATE_USER);
            case DELETE_USER -> Optional.of(Permission.DELETE_USER);
            case SUSPEND_USER -> Optional.of(Permission.SUSPEND_USER);
            case VIEW_ALL_USERS -> Optional.of(Permission.VIEW_ALL_USERS);
            case MODIFY_USER_ROLES -> Optional.of(Permission.MODIFY_USER_ROLES);
            case COURSE_MANAGEMENT -> Optional.of(Permission.MANAGE_COURSES);
            case MANAGE_COURSES -> Optional.of(Permission.MANAGE_COURSES);
            case CREATE_COURSE -> Optional.of(Permission.CREATE_COURSE);
            case UPDATE_COURSE -> Optional.of(Permission.UPDATE_COURSE);
            case DELETE_COURSE -> Optional.of(Permission.DELETE_COURSE);
            case VIEW_ALL_COURSES -> Optional.of(Permission.VIEW_ALL_COURSES);
            case MANAGE_ENROLLMENTS -> Optional.of(Permission.MANAGE_ENROLLMENTS);
            case ENROLL_STUDENT -> Optional.of(Permission.ENROLL_STUDENT);
            case DROP_STUDENT -> Optional.of(Permission.DROP_STUDENT);
            case VIEW_ALL_ENROLLMENTS -> Optional.of(Permission.VIEW_ALL_ENROLLMENTS);
            case TEACHER_MANAGEMENT -> Optional.of(Permission.VIEW_ALL_COURSES);
            case MANAGE_GRADING -> Optional.of(Permission.VIEW_ALL_GRADES);
            case VIEW_ALL_GRADES -> Optional.of(Permission.VIEW_ALL_GRADES);
            case MODIFY_GRADES -> Optional.of(Permission.MODIFY_GRADES);
            case CREATE_ASSIGNMENT -> Optional.of(Permission.CREATE_ASSIGNMENT);
            case MANAGE_TEACHERS -> Optional.of(Permission.MANAGE_COURSES); // Map to appropriate permission
            case VIEW_TEACHER_ASSIGNMENTS -> Optional.of(Permission.VIEW_ALL_COURSES);
            case STUDENT_MANAGEMENT -> Optional.of(Permission.MANAGE_USERS);
            case PARENT_MANAGEMENT -> Optional.of(Permission.MANAGE_USERS);
            case MANAGE_PARENTS -> Optional.of(Permission.MANAGE_USERS);
            case LINK_PARENT_STUDENT -> Optional.of(Permission.MANAGE_ENROLLMENTS);
            case VIEW_PARENT_LINKS -> Optional.of(Permission.VIEW_ALL_ENROLLMENTS);
            case MANAGE_TENANTS -> Optional.of(Permission.MANAGE_TENANTS);
            case MANAGE_ROLES -> Optional.of(Permission.MANAGE_ROLES);
            case VIEW_ALL_TENANTS -> Optional.of(Permission.VIEW_ALL_TENANTS);
            case SYSTEM_SETTINGS -> Optional.of(Permission.MANAGE_SETTINGS);
            case REPORTS_VIEW -> Optional.of(Permission.VIEW_REPORTS);
            case VIEW_REPORTS -> Optional.of(Permission.VIEW_REPORTS);
            case REPORTS_EXPORT -> Optional.of(Permission.EXPORT_DATA);
            case EXPORT_DATA -> Optional.of(Permission.EXPORT_DATA);
            case MANAGE_SETTINGS -> Optional.of(Permission.MANAGE_SETTINGS);
            case MANAGE_PERMISSIONS -> Optional.of(Permission.MANAGE_PERMISSIONS);
            case VIEW_AUDIT_LOGS -> Optional.of(Permission.VIEW_AUDIT_LOGS);
            case GRADE_ASSIGNMENT -> Optional.of(Permission.GRADE_ASSIGNMENT);
        };
    }
}
