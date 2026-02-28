-- ============================================================
-- Create Specialization Tables
-- ============================================================

-- Teachers table
CREATE TABLE IF NOT EXISTS teachers (
  user_id UUID PRIMARY KEY,
  employee_id VARCHAR(100) UNIQUE NOT NULL,
  department VARCHAR(200),
  hire_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_teachers_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);

-- Parents table
CREATE TABLE IF NOT EXISTS parents (
  user_id UUID PRIMARY KEY,
  phone_number VARCHAR(50),
  address TEXT,
  emergency_contact VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_parents_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
  user_id UUID PRIMARY KEY,
  student_id VARCHAR(100) UNIQUE NOT NULL,
  grade_level VARCHAR(50) NOT NULL,
  date_of_birth DATE NOT NULL,
  guardian_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_students_guardian FOREIGN KEY (guardian_id)
    REFERENCES parents(user_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_teachers_employee ON teachers(employee_id);
CREATE INDEX IF NOT EXISTS idx_students_guardian ON students(guardian_id);
