-- V48__user_role_tracking.sql
-- Migration: Default role for new users is now 'USER' (instead of 'MEMBER').
-- Adds role change audit columns to the app_user table.
--
-- Impact on existing data:
--   - Existing users with role = 'MEMBER' are UNCHANGED (no backfill).
--   - The role column default is updated to 'USER' for any future INSERT
--     that does not explicitly supply a role value.
--   - role_changed_at / role_changed_by track every admin role-upgrade event.
-- ────────────────────────────────────────────────────────────────────────────

-- 1. Change column default from MEMBER to USER
ALTER TABLE IF EXISTS manacommunity.app_user
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE IF EXISTS app_user
    ALTER COLUMN role SET DEFAULT 'USER';

-- 2. Add audit columns for role change tracking
ALTER TABLE IF EXISTS manacommunity.app_user
    ADD COLUMN IF NOT EXISTS role_changed_at  TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS role_changed_by  BIGINT;

ALTER TABLE IF EXISTS app_user
    ADD COLUMN IF NOT EXISTS role_changed_at  TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS role_changed_by  BIGINT;

-- 3. Optional soft FK comment (constraint intentionally not added to
--    avoid blocking user deletes or circular dependency issues)
COMMENT ON COLUMN app_user.role_changed_by IS
    'References app_user.id of the admin who last changed this user''s role. Not enforced as FK.';

COMMENT ON COLUMN app_user.role_changed_at IS
    'Timestamp of the last role change made by an admin.';

COMMENT ON COLUMN app_user.role IS
    'Current role of the user. Defaults to USER on registration. '
    'Can be upgraded to MEMBER, STAFF, CASHIER, VENDOR, SPORTS_ADMIN, ADMIN, COMMUNITY_ADMIN '
    'by an admin via PUT /api/users/{id}/role.';
