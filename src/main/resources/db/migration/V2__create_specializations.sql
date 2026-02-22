-- ============================================================
-- K-12 School Management System - Specialization Tables
-- Version: 2
-- Description: Creates tables for Teacher, Student, Parent, and Admin specializations
-- ============================================================

-- ============================================================
-- Teacher Profiles
-- ============================================================
CREATE TABLE teacher_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    bio TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED')),
    hire_date TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Indexes for teacher_profiles
CREATE INDEX idx_teacher_user_id ON teacher_profiles(user_id);
CREATE INDEX idx_teacher_department ON teacher_profiles(department);
CREATE INDEX idx_teacher_status ON teacher_profiles(status);
CREATE INDEX idx_teacher_employee_id ON teacher_profiles(employee_id);

-- ============================================================
-- Teacher Qualifications
-- ============================================================
CREATE TABLE teacher_qualifications (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    degree VARCHAR(200) NOT NULL,
    institution VARCHAR(200) NOT NULL,
    year_awarded INTEGER NOT NULL CHECK (year_awarded >= 1950 AND year_awarded <= EXTRACT(YEAR FROM CURRENT_DATE) + 1),
    field_of_study VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teacher_qualifications_teacher_id ON teacher_qualifications(teacher_id);

-- ============================================================
-- Teacher Subjects
-- ============================================================
CREATE TABLE teacher_subjects (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    subject VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(teacher_id, subject)
);

CREATE INDEX idx_teacher_subjects_teacher_id ON teacher_subjects(teacher_id);

-- ============================================================
-- Teacher Course Assignments
-- ============================================================
CREATE TABLE teacher_course_assignments (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    course_id UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(teacher_id, course_id)
);

CREATE INDEX idx_teacher_assignments_teacher_id ON teacher_course_assignments(teacher_id);
CREATE INDEX idx_teacher_assignments_course_id ON teacher_course_assignments(course_id);

-- ============================================================
-- Student Profiles
-- ============================================================
CREATE TABLE student_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    grade_level VARCHAR(10) NOT NULL CHECK (grade_level IN ('KINDERGARTEN', 'GRADE_1', 'GRADE_2', 'GRADE_3', 'GRADE_4', 'GRADE_5', 'GRADE_6', 'GRADE_7', 'GRADE_8', 'GRADE_9', 'GRADE_10', 'GRADE_11', 'GRADE_12')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'GRADUATED', 'WITHDRAWN')),
    enrollment_date TIMESTAMP NOT NULL DEFAULT NOW(),
    guardian_name VARCHAR(200) NOT NULL,
    guardian_phone VARCHAR(20) NOT NULL,
    guardian_email VARCHAR(255) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    emergency_contact_phone VARCHAR(20),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Indexes for student_profiles
CREATE INDEX idx_student_user_id ON student_profiles(user_id);
CREATE INDEX idx_student_grade_level ON student_profiles(grade_level);
CREATE INDEX idx_student_status ON student_profiles(status);

-- ============================================================
-- Student Enrollments
-- ============================================================
CREATE TABLE student_enrollments (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    course_id UUID NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(student_id, course_id)
);

CREATE INDEX idx_student_enrollments_student_id ON student_enrollments(student_id);
CREATE INDEX idx_student_enrollments_course_id ON student_enrollments(course_id);

-- ============================================================
-- Student Grades
-- ============================================================
CREATE TABLE student_grades (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    course_id UUID NOT NULL,
    letter_grade VARCHAR(3) NOT NULL CHECK (letter_grade ~ '^[A-D][+\\-]?$|^F$'),
    numeric_grade DECIMAL(5,2) NOT NULL CHECK (numeric_grade >= 0 AND numeric_grade <= 100),
    graded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    graded_by VARCHAR(200) NOT NULL,
    UNIQUE(student_id, course_id)
);

CREATE INDEX idx_student_grades_student_id ON student_grades(student_id);
CREATE INDEX idx_student_grades_course_id ON student_grades(course_id);

-- ============================================================
-- Student Assignments
-- ============================================================
CREATE TABLE student_assignments (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    assignment_id UUID NOT NULL,
    course_id UUID NOT NULL,
    submission_url VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(student_id, assignment_id)
);

CREATE INDEX idx_student_assignments_student_id ON student_assignments(student_id);
CREATE INDEX idx_student_assignments_course_id ON student_assignments(course_id);

-- ============================================================
-- Parent Profiles
-- ============================================================
CREATE TABLE parent_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_preference VARCHAR(20) NOT NULL DEFAULT 'EMAIL' CHECK (contact_preference IN ('EMAIL', 'SMS', 'PHONE', 'EMAIL_AND_SMS')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    registration_date TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Indexes for parent_profiles
CREATE INDEX idx_parent_user_id ON parent_profiles(user_id);
CREATE INDEX idx_parent_status ON parent_profiles(status);

-- ============================================================
-- Parent Relationships
-- ============================================================
CREATE TABLE parent_relationships (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL REFERENCES parent_profiles(id) ON DELETE CASCADE,
    relationship VARCHAR(50) NOT NULL CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN', 'STEP_FATHER', 'STEP_MOTHER', 'GRANDPARENT', 'OTHER')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(parent_id, relationship)
);

CREATE INDEX idx_parent_relationships_parent_id ON parent_relationships(parent_id);

-- ============================================================
-- Parent-Student Links
-- ============================================================
CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL REFERENCES parent_profiles(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    relationship VARCHAR(50) NOT NULL CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN', 'STEP_FATHER', 'STEP_MOTHER', 'GRANDPARENT', 'OTHER')),
    linked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(parent_id, student_id)
);

CREATE INDEX idx_parent_student_links_parent_id ON parent_student_links(parent_id);
CREATE INDEX idx_parent_student_links_student_id ON parent_student_links(student_id);

-- ============================================================
-- Admin Profiles
-- ============================================================
CREATE TABLE admin_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(20) NOT NULL CHECK (permission_level IN ('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'SCHOOL_ADMIN', 'COURSE_ADMIN')),
    scope VARCHAR(50) CHECK (scope IN ('ALL', 'DEPARTMENT', 'GRADE_LEVEL', 'COURSE')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    appointment_date TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Indexes for admin_profiles
CREATE INDEX idx_admin_user_id ON admin_profiles(user_id);
CREATE INDEX idx_admin_permission_level ON admin_profiles(permission_level);
CREATE INDEX idx_admin_status ON admin_profiles(status);

-- ============================================================
-- Admin Permissions (additional permissions beyond role defaults)
-- ============================================================
CREATE TABLE admin_permissions (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES admin_profiles(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(admin_id, permission)
);

CREATE INDEX idx_admin_permissions_admin_id ON admin_permissions(admin_id);

-- ============================================================
-- Functions and Triggers for updated_at
-- ============================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_teacher_profiles_updated_at
    BEFORE UPDATE ON teacher_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_student_profiles_updated_at
    BEFORE UPDATE ON student_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_parent_profiles_updated_at
    BEFORE UPDATE ON parent_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_admin_profiles_updated_at
    BEFORE UPDATE ON admin_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Comments for documentation
-- ============================================================

COMMENT ON TABLE teacher_profiles IS 'Aggregate root for Teacher specialization. Contains teaching-specific data linked to a user.';
COMMENT ON TABLE teacher_qualifications IS 'Value objects representing teacher qualifications (degrees, certifications).';
COMMENT ON TABLE teacher_subjects IS 'Subjects that a teacher is qualified to teach.';
COMMENT ON TABLE teacher_course_assignments IS 'Courses assigned to a teacher.';

COMMENT ON TABLE student_profiles IS 'Aggregate root for Student specialization. Contains learning-specific data linked to a user.';
COMMENT ON TABLE student_enrollments IS 'Courses a student is currently enrolled in.';
COMMENT ON TABLE student_grades IS 'Grades for students in specific courses.';
COMMENT ON TABLE student_assignments IS 'Assignment submissions tracked for students.';

COMMENT ON TABLE parent_profiles IS 'Aggregate root for Parent specialization. Contains parenting-specific data linked to a user.';
COMMENT ON TABLE parent_relationships IS 'Relationship types a parent has (e.g., father, mother, guardian).';
COMMENT ON TABLE parent_student_links IS 'Links between parents and students, showing the relationship.';

COMMENT ON TABLE admin_profiles IS 'Aggregate root for Admin specialization. Contains administration-specific data linked to a user.';
COMMENT ON TABLE admin_permissions IS 'Additional permissions granted to an admin beyond their role defaults.';
