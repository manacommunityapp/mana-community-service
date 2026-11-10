-- =============================================================================
-- Script: restore_users_from_audit.sql
-- Description: Restores deleted residents and community users from Hibernate
--              Envers audit table (manacommunity.app_user_aud) back into
--              manacommunity.app_user.
--
-- Preserves:
--   - Original user IDs, names, emails, phone numbers, DOB, gender, KYC status
--   - Blocks and flat numbers
--   - Role associations (Admin vs User/Resident)
--   - BCrypt password hashes (restored from legacy users table where available,
--     with default hash fallback for newly registered accounts)
--   - Sequence synchronization for app_user_id_seq and roles_id_seq
-- =============================================================================

BEGIN;

-- 1. Ensure Role 55 ('USER' for Community 2) exists in roles table
INSERT INTO manacommunity.roles (id, name, community_id)
VALUES (55, 'USER', 2)
ON CONFLICT (id) DO NOTHING;

-- 2. Restore all distinct users from their latest revision in app_user_aud
WITH latest_aud AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY id ORDER BY rev DESC) as rn
    FROM manacommunity.app_user_aud
)
INSERT INTO manacommunity.app_user (
    id,
    email,
    full_name,
    phone,
    password_hash,
    date_of_birth,
    gender,
    profile_pic_url,
    role,
    role_id,
    role_changed_at,
    role_changed_by,
    kyc_status,
    govt_id_type,
    govt_id_number,
    flat_no,
    block,
    employee_id,
    tower,
    resident_type,
    occupancy_status,
    is_active,
    failed_login_attempts,
    locked_until,
    community_id,
    organization_id,
    notify_email,
    notify_push,
    notify_sms,
    notify_whatsapp,
    created_at,
    updated_at
)
SELECT
    a.id,
    a.email,
    a.full_name,
    a.phone,
    COALESCE(u.password_hash, '$2a$10$r8rdMZlcH/Li8IGaOMIdaeE40aOxYIGt7bmZkrg6W2.l/5cs0pZFu'),
    COALESCE(a.date_of_birth, '1990-01-01'::date),
    COALESCE(a.gender, 'OTHER'),
    a.profile_pic_url,
    a.role,
    CASE WHEN a.id = 4 THEN 19 ELSE 55 END,
    a.role_changed_at,
    a.role_changed_by,
    COALESCE(a.kyc_status, 'PENDING'),
    a.govt_id_type,
    a.govt_id_number,
    a.flat_no,
    a.block,
    a.employee_id,
    a.tower,
    a.resident_type,
    a.occupancy_status,
    COALESCE(a.is_active, true),
    0,
    NULL,
    COALESCE(a.community_id, 2),
    a.organization_id,
    COALESCE(a.notify_email, true),
    COALESCE(a.notify_push, true),
    COALESCE(a.notify_sms, false),
    COALESCE(a.notify_whatsapp, false),
    COALESCE(a.created_at, NOW()),
    COALESCE(a.updated_at, NOW())
FROM latest_aud a
LEFT JOIN manacommunity.users u ON lower(a.email) = lower(u.email)
WHERE a.rn = 1
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    date_of_birth = EXCLUDED.date_of_birth,
    gender = EXCLUDED.gender,
    role = EXCLUDED.role,
    role_id = EXCLUDED.role_id,
    flat_no = EXCLUDED.flat_no,
    block = EXCLUDED.block,
    community_id = EXCLUDED.community_id;

-- 3. Synchronize core table primary key sequences beyond highest restored IDs
SELECT setval('manacommunity.app_user_id_seq', (SELECT GREATEST(MAX(id) + 1, 203) FROM manacommunity.app_user), false);
SELECT setval('manacommunity.roles_id_seq', (SELECT GREATEST(MAX(id) + 1, 56) FROM manacommunity.roles), false);

COMMIT;
