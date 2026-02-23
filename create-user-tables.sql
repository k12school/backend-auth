-- ============================================================
-- Create User Events Table for Event Sourcing
-- ============================================================

-- Create user_events table (event store)
CREATE TABLE IF NOT EXISTS user_events (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_data BYTEA NOT NULL,
    CONSTRAINT uq_user_event_version UNIQUE (user_id, version)
);

-- Create indexes for user_events
CREATE INDEX IF NOT EXISTS idx_user_events_occurred_at ON user_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_user_events_user_id ON user_events(user_id);

-- Create users projection table (read model)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL, -- Stored as comma-separated values: "SUPER_ADMIN,ADMIN"
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    name VARCHAR(200) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

COMMENT ON TABLE user_events IS 'Event store for User aggregate using Event Sourcing';
COMMENT ON TABLE users IS 'Read projection of User data for queries';
