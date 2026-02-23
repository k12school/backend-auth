-- Tenant Projection Table for Queries
-- Read model built from events
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subdomain VARCHAR(63) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for name uniqueness checks
CREATE INDEX idx_tenants_name ON tenants(name);

-- Index for subdomain lookups
CREATE INDEX idx_tenants_subdomain ON tenants(subdomain);

-- Index for status queries
CREATE INDEX idx_tenants_status ON tenants(status);

-- Comment for documentation
COMMENT ON TABLE tenants IS 'Read model projection for Tenant - built from event stream';
COMMENT ON COLUMN tenants.version IS 'Last applied event version';
