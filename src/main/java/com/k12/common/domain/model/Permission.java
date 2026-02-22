package com.k12.common.domain.model;

/**
 * Represents individual permissions in the system.
 * These permissions can be assigned to roles and users.
 */
public enum Permission {
    // User management
    MANAGE_USERS,
    CREATE_USER,
    DELETE_USER,
    SUSPEND_USER,
    VIEW_ALL_USERS,
    MODIFY_USER_ROLES,

    // Course management
    MANAGE_COURSES,
    CREATE_COURSE,
    UPDATE_COURSE,
    DELETE_COURSE,
    VIEW_ALL_COURSES,

    // Enrollment
    MANAGE_ENROLLMENTS,
    ENROLL_STUDENT,
    DROP_STUDENT,
    VIEW_ALL_ENROLLMENTS,

    // Grading
    CREATE_ASSIGNMENT,
    GRADE_ASSIGNMENT,
    VIEW_GRADES,
    UPDATE_GRADES,
    VIEW_ALL_GRADES,
    MODIFY_GRADES,

    // Parent specific
    VIEW_CHILD_GRADES,
    VIEW_CHILD_ATTENDANCE,
    VIEW_CHILD_ASSIGNMENTS,

    // Teacher specific
    VIEW_ASSIGNED_COURSES,
    UPDATE_COURSE_CONTENT,
    MANAGE_OWN_COURSES,

    // Student specific
    VIEW_OWN_GRADES,
    SUBMIT_ASSIGNMENT,
    VIEW_OWN_ASSIGNMENTS,

    // Admin specific
    MANAGE_TENANTS,
    MANAGE_ROLES,
    VIEW_ALL_TENANTS,
    MANAGE_PERMISSIONS,

    // Reports
    VIEW_REPORTS,
    EXPORT_DATA,

    // Settings
    MANAGE_SETTINGS,

    // Audit
    VIEW_AUDIT_LOGS
}
