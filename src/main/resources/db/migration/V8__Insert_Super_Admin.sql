-- ============================================================
-- Insert Super Admin User
-- ============================================================
-- This migration creates a default super admin user with all permissions.
--
-- Super Admin Credentials:
-- Email: admin@k12.com
-- Password: admin123
--
-- The super admin has ALL permissions and belongs to a default tenant.
--
-- NOTE: This directly inserts into projection tables for immediate functionality.
-- When event persistence is fully implemented, this should be updated to use events.

-- Generate a fixed UUID for the super admin (for consistency)
DO $$
DECLARE
    v_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    v_user_id UUID := '00000000-0000-0000-0000-000000000002';
    v_now TIMESTAMPTZ := CURRENT_TIMESTAMP;
BEGIN
    -- Insert default tenant if not exists
    INSERT INTO tenants (id, name, subdomain, status, version, created_at, updated_at)
    VALUES (
        v_tenant_id,
        'Default Tenant',
        'default',
        'ACTIVE',
        1,
        v_now,
        v_now
    )
    ON CONFLICT (id) DO NOTHING;

    -- Insert super admin user
    INSERT INTO users (id, email, password_hash, roles, status, name, created_at, updated_at)
    VALUES (
        v_user_id,
        'admin@k12.com',
        '$2y$10$w1vuFZtUrAhZ8WjsVBxU/.51EtfYhG8KX7Vd2jUbFe/l79LWV9eAm', -- admin123
        'SUPER_ADMIN',
        'ACTIVE',
        'Super Admin',
        v_now,
        v_now
    )
    ON CONFLICT (email) DO NOTHING;

    -- Insert admin profile with all permissions
    /*
    INSERT INTO admins (user_id, permissions, status, created_at, updated_at)
    VALUES (
        v_user_id,
        'USER_MANAGEMENT,COURSE_MANAGEMENT,TEACHER_MANAGEMENT,STUDENT_MANAGEMENT,PARENT_MANAGEMENT,SYSTEM_SETTINGS,REPORTS_VIEW,REPORTS_EXPORT,MANAGE_USERS,CREATE_USER,DELETE_USER,SUSPEND_USER,VIEW_ALL_USERS,MODIFY_USER_ROLES,MANAGE_COURSES,CREATE_COURSE,UPDATE_COURSE,DELETE_COURSE,VIEW_ALL_COURSES,MANAGE_ENROLLMENTS,ENROLL_STUDENT,DROP_STUDENT,VIEW_ALL_ENROLLMENTS,MANAGE_GRADING,VIEW_ALL_GRADES,MODIFY_GRADES,CREATE_ASSIGNMENT,MANAGE_TEACHERS,VIEW_TEACHER_ASSIGNMENTS,MANAGE_PARENTS,LINK_PARENT_STUDENT,VIEW_PARENT_LINKS,MANAGE_TENANTS,MANAGE_ROLES,VIEW_ALL_TENANTS,VIEW_REPORTS,EXPORT_DATA,MANAGE_SETTINGS,MANAGE_PERMISSIONS,VIEW_AUDIT_LOGS,GRADE_ASSIGNMENT',
        'ACTIVE',
        v_now,
        v_now
    )
    ON CONFLICT (user_id) DO NOTHING;
     */

    RAISE NOTICE 'Super admin created successfully. Email: admin@k12.com, Password: admin123';
END $$;
