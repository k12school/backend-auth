-- Tenant Events Table for Event Sourcing
-- Stores all domain events for the Tenant aggregate
CREATE TABLE tenant_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure version uniqueness per tenant (optimistic locking)
    CONSTRAINT uq_tenant_event_version UNIQUE (tenant_id, version)
);

-- Index for querying events by tenant_id
CREATE INDEX idx_tenant_events_tenant_id ON tenant_events(tenant_id);

-- Index for querying by subdomain (for uniqueness checks)
CREATE INDEX idx_tenant_events_occurred_at ON tenant_events(occurred_at);

-- Comment for documentation
COMMENT ON TABLE tenant_events IS 'Event store for Tenant aggregate - stores all domain events';
COMMENT ON COLUMN tenant_events.event_type IS 'Type of event (TenantCreated, TenantSuspended, etc.)';
COMMENT ON COLUMN tenant_events.event_data IS 'JSON serialized event data';
COMMENT ON COLUMN tenant_events.version IS 'Aggregate version for optimistic locking';
