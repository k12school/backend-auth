-- ============================================================
-- Insert SUPER_ADMIN User
-- Email: admin@k12.com
-- Password: admin123
-- Role: SUPER_ADMIN
-- ============================================================

-- Note: This inserts directly into the users projection table.
-- The event store entry requires Kryo serialization which must be done
-- from the application layer.

DO $$
DECLARE
    user_id UUID := '550e8400-e29b-41d4-a716-446655440000';
    version BIGINT := 1;
BEGIN
    -- Insert into users projection table
    INSERT INTO users (
        id,
        email,
        password_hash,
        roles,
        status,
        name,
        version,
        created_at,
        updated_at
    ) VALUES (
        user_id,
        'admin@k12.com',
        '$2b$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6',
        'SUPER_ADMIN',
        'ACTIVE',
        'Super Administrator',
        version,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    RAISE NOTICE 'SUPER_ADMIN user created successfully';
    RAISE NOTICE 'Email: admin@k12.com';
    RAISE NOTICE 'Password: admin123';
    RAISE NOTICE 'User ID: %', user_id;
END $$;

-- Verify the user was created
\echo 'User created:'
SELECT id, email, roles, status, name, created_at FROM users WHERE email = 'admin@k12.com';
