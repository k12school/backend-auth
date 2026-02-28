-- ============================================================
-- Add Tenant Association to Users
-- ============================================================

-- Update existing users to belong to default tenant first
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

-- Drop existing foreign key constraint (it has ON DELETE SET NULL which is incompatible with NOT NULL)
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_tenant;

-- Make tenant_id NOT NULL with default value
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';

-- Add foreign key constraint without ON DELETE SET NULL
ALTER TABLE users ADD CONSTRAINT fk_users_tenant
  FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- Index already exists from V6, but ensure it's there
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);

-- Add tenant_id to user_events for event sourcing
ALTER TABLE user_events ADD COLUMN IF NOT EXISTS tenant_id UUID;
