-- Add tenant_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Add index for tenant_id lookups
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

-- Add foreign key constraint to tenants table
ALTER TABLE users
ADD CONSTRAINT fk_users_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;

COMMENT ON COLUMN users.tenant_id IS 'Associated tenant ID for multi-tenancy support';
