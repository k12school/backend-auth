-- ============================================================
-- Admins Table for User Specialization
-- ============================================================
-- Admin is a specialization of User with additional permissions.
-- Each Admin has a 1:1 relationship with a User.
-- The user_id in admins table references the id in users table.

CREATE TABLE IF NOT EXISTS admins (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    permissions TEXT NOT NULL, -- Comma-separated permissions: "USER_MANAGEMENT,COURSE_MANAGEMENT"
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- Admin status: ACTIVE, SUSPENDED, REVOKED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for permission queries (simple text index)
CREATE INDEX IF NOT EXISTS idx_admins_permissions ON admins(permissions);

-- Comment for documentation
COMMENT ON TABLE admins IS 'Admin specialization - stores admin-specific data linked to users';
COMMENT ON COLUMN admins.user_id IS 'Foreign key reference to users table (1:1 relationship)';
COMMENT ON COLUMN admins.permissions IS 'Comma-separated list of Permission enum values';
COMMENT ON COLUMN admins.created_at IS 'Timestamp when admin was created';
COMMENT ON COLUMN admins.updated_at IS 'Timestamp when admin record was last updated';

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_admins_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_admins_updated_at
    BEFORE UPDATE ON admins
    FOR EACH ROW
    EXECUTE FUNCTION update_admins_updated_at();
