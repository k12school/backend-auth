-- ============================================================
-- Update Super Admin Role
-- ============================================================
-- This migration ensures the admin@k12.com user has the SUPER_ADMIN role.
-- It fixes databases that were created before V8 was updated.

UPDATE users
SET roles = 'SUPER_ADMIN'
WHERE email = 'admin@k12.com' AND roles = 'ADMIN';
