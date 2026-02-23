-- Convert event_data from JSONB to BYTEA for Kryo binary serialization
-- This migration changes the column type to store binary serialized events

-- Step 1: Add a new temporary column for binary data
ALTER TABLE tenant_events ADD COLUMN event_data_binary BYTEA;

-- Step 2: Copy existing JSON data to binary (JSON will remain readable until next app start)
-- Note: Existing events can't be automatically converted to Kryo format
-- They will be re-published when the tenant is next modified
UPDATE tenant_events SET event_data_binary = event_data::text::bytea WHERE event_data_binary IS NULL;

-- Step 3: Drop the old column and rename the new one
ALTER TABLE tenant_events DROP COLUMN event_data;
ALTER TABLE tenant_events RENAME COLUMN event_data_binary TO event_data;

-- Step 4: Make the column NOT NULL (it should already be populated)
ALTER TABLE tenant_events ALTER COLUMN event_data SET NOT NULL;

-- Update comment
COMMENT ON COLUMN tenant_events.event_data IS 'Kryo binary serialized event data';
