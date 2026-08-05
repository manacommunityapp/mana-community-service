-- ============================================================================
-- V39__composite_indexes.sql
-- Adds composite indexes for high-frequency query paths across the
-- manacommunity schema.
--
-- All statements use CREATE INDEX IF NOT EXISTS so this migration is safe to
-- run even if some indexes were created manually beforehand.
-- ============================================================================

-- ── app_user ─────────────────────────────────────────────────────────────────
-- Covers: community member list, active user lookup
CREATE INDEX IF NOT EXISTS idx_user_community_active
    ON manacommunity.app_user (community_id, is_active);

-- Covers: KYC status filtering per community (admin approval flows)
CREATE INDEX IF NOT EXISTS idx_user_community_kyc
    ON manacommunity.app_user (community_id, kyc_status);

-- Covers: login / email lookup (auth hot-path)
CREATE INDEX IF NOT EXISTS idx_user_email
    ON manacommunity.app_user (email);

-- ── sports_event ──────────────────────────────────────────────────────────────
-- Covers: event listing by community + status (most-common list query)
CREATE INDEX IF NOT EXISTS idx_event_community_status
    ON manacommunity.sports_event (community_id, status);

-- Covers: "registration open" window checks
CREATE INDEX IF NOT EXISTS idx_event_registration_dates
    ON manacommunity.sports_event (registration_date_start, registration_date_end);

-- ── sports_event_registration ─────────────────────────────────────────────────
-- Covers: admin registration list filtered by event + status
CREATE INDEX IF NOT EXISTS idx_reg_event_status
    ON manacommunity.sports_event_registration (event_id, status);

-- Covers: community-level registration counts / dashboards
CREATE INDEX IF NOT EXISTS idx_reg_community_status
    ON manacommunity.sports_event_registration (community_id, status);

-- Covers: user''s own registration history (profile page)
CREATE INDEX IF NOT EXISTS idx_reg_user_event
    ON manacommunity.sports_event_registration (user_id, event_id);

-- ── role_permissions ──────────────────────────────────────────────────────────
-- Covers: per-user permission lookup (security filter hot-path)
CREATE INDEX IF NOT EXISTS idx_roleperm_user_role
    ON manacommunity.role_permissions (user_id, role_id);

-- Covers: role-level permission key scan
CREATE INDEX IF NOT EXISTS idx_roleperm_role_key
    ON manacommunity.role_permissions (role_id, permission_key);

-- ── notification ──────────────────────────────────────────────────────────────
-- Covers: unread notification badge count (partial index — only unread rows)
CREATE INDEX IF NOT EXISTS idx_notification_user_unread
    ON manacommunity.notification (user_id, is_read)
    WHERE is_read = FALSE;

-- ── audit_log (supplements existing indexes) ─────────────────────────────────
-- Covers: entity-level audit trail lookup (admin audit viewer)
CREATE INDEX IF NOT EXISTS idx_audit_entity
    ON manacommunity.audit_log (entity_name, entity_id, created_at DESC);
