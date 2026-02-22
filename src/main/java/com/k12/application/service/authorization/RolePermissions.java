package com.k12.application.service.authorization;

import com.k12.domain.model.authorization.Permission;
import com.k12.domain.model.user.UserRole;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps user roles to their default permissions.
 * Provides methods to query permissions for roles.
 */
public final class RolePermissions {

    private RolePermissions() {
        // Utility class - prevent instantiation
    }

    /**
     * Default permissions for each role.
     */
    private static final java.util.Map<UserRole, Set<Permission>> DEFAULT_PERMISSIONS = java.util.Map.of(
            UserRole.SUPER_ADMIN, EnumSet.allOf(Permission.class),
            UserRole.ADMIN,
                    Set.of(
                            // User management
                            Permission.MANAGE_USERS,
                            Permission.VIEW_ALL_USERS,
                            Permission.CREATE_USER,
                            Permission.MODIFY_USER_ROLES,
                            // Course management
                            Permission.MANAGE_COURSES,
                            Permission.VIEW_ALL_COURSES,
                            Permission.CREATE_COURSE,
                            Permission.UPDATE_COURSE,
                            // Enrollment
                            Permission.MANAGE_ENROLLMENTS,
                            Permission.VIEW_ALL_ENROLLMENTS,
                            Permission.ENROLL_STUDENT,
                            Permission.DROP_STUDENT,
                            // Grading
                            Permission.VIEW_ALL_GRADES,
                            // Reports
                            Permission.VIEW_REPORTS,
                            Permission.EXPORT_DATA,
                            // Settings
                            Permission.MANAGE_SETTINGS,
                            // Audit
                            Permission.VIEW_AUDIT_LOGS),
            UserRole.TEACHER,
                    Set.of(
                            // Course viewing
                            Permission.VIEW_ASSIGNED_COURSES,
                            Permission.UPDATE_COURSE_CONTENT,
                            Permission.MANAGE_OWN_COURSES,
                            // Assignments and grading
                            Permission.CREATE_ASSIGNMENT,
                            Permission.GRADE_ASSIGNMENT,
                            Permission.VIEW_GRADES,
                            // Student viewing
                            Permission.VIEW_ALL_GRADES),
            UserRole.PARENT,
                    Set.of(
                            // Child-related viewing
                            Permission.VIEW_CHILD_GRADES,
                            Permission.VIEW_CHILD_ATTENDANCE,
                            Permission.VIEW_CHILD_ASSIGNMENTS),
            UserRole.STUDENT,
                    Set.of(
                            // Own data viewing
                            Permission.VIEW_OWN_GRADES, Permission.VIEW_OWN_ASSIGNMENTS, Permission.SUBMIT_ASSIGNMENT));

    /**
     * Gets the default permissions for a single role.
     *
     * @param role The role to get permissions for
     * @return Set of permissions for the role
     */
    public static Set<Permission> getPermissionsForRole(UserRole role) {
        return DEFAULT_PERMISSIONS.getOrDefault(role, Set.of());
    }

    /**
     * Gets the combined permissions for multiple roles.
     * Useful for users with multiple roles.
     *
     * @param roles Set of roles
     * @return Combined set of all permissions
     */
    public static Set<Permission> getPermissionsForRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }

        return roles.stream()
                .flatMap(role -> getPermissionsForRole(role).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a role has a specific permission.
     *
     * @param role The role to check
     * @param permission The permission to check for
     * @return true if the role has the permission
     */
    public static boolean roleHasPermission(UserRole role, Permission permission) {
        return getPermissionsForRole(role).contains(permission);
    }

    /**
     * Checks if a set of roles has a specific permission.
     *
     * @param roles Set of roles to check
     * @param permission The permission to check for
     * @return true if any of the roles has the permission
     */
    public static boolean rolesHavePermission(Set<UserRole> roles, Permission permission) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        return roles.stream().anyMatch(role -> roleHasPermission(role, permission));
    }
}
