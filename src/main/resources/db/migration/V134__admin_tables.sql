-- ──────────────────────────────────────────────────────────────
-- Admin Module — New Tables
-- Run this once against your PostgreSQL DB.
-- If you are using Flyway, name this file V4__admin_tables.sql
-- (or V5__ if V4 already exists — check your migration history first).
-- ──────────────────────────────────────────────────────────────

-- ── 1. content_report ─────────────────────────────────────────
--    Stores community member reports of inappropriate posts / users.
CREATE TABLE IF NOT EXISTS content_report (
    id             BIGSERIAL    PRIMARY KEY,
    reporter_id    BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    community_id   BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    target_type    VARCHAR(20)  NOT NULL,               -- POST | COMMENT | USER
    target_id      BIGINT       NOT NULL,
    target_content VARCHAR(500),
    target_author  VARCHAR(100),
    reason         VARCHAR(40)  NOT NULL,               -- SPAM | HARASSMENT | INAPPROPRIATE | ...
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | RESOLVED | DISMISSED
    resolved_by_id BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    resolved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_report_community_status
    ON content_report (community_id, status);
CREATE INDEX IF NOT EXISTS idx_report_target
    ON content_report (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_report_reporter
    ON content_report (reporter_id);

-- ── 2. announcement ───────────────────────────────────────────
--    Admin-created community-wide announcements.
CREATE TABLE IF NOT EXISTS announcement (
    id           BIGSERIAL    PRIMARY KEY,
    community_id BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    author_id    BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title        VARCHAR(120) NOT NULL,
    content      TEXT         NOT NULL,
    priority     VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',  -- NORMAL | URGENT
    is_pinned    BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ann_community_created
    ON announcement (community_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ann_community_pinned
    ON announcement (community_id, is_pinned);

-- ── 3. community_settings ─────────────────────────────────────
--    Optional per-community feature flags and metadata.
--    One row per community, created on first settings save.
CREATE TABLE IF NOT EXISTS community_settings (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT    NOT NULL UNIQUE REFERENCES community(id) ON DELETE CASCADE,
    description         VARCHAR(500),
    address             VARCHAR(200),
    max_members         INT       NOT NULL DEFAULT 500,
    feature_marketplace BOOLEAN   NOT NULL DEFAULT TRUE,
    feature_sports      BOOLEAN   NOT NULL DEFAULT TRUE,
    feature_auction     BOOLEAN   NOT NULL DEFAULT TRUE,
    feature_jobs        BOOLEAN   NOT NULL DEFAULT TRUE,
    feature_polls       BOOLEAN   NOT NULL DEFAULT TRUE
);

-- ── 4. Add invite_code column to community if missing ─────────
--    The Community entity already has this field, but the original
--    schema comment said it was "nullable, app-level only". This
--    ensures the column exists with a unique constraint.
ALTER TABLE community
    ADD COLUMN IF NOT EXISTS invite_code VARCHAR(20) UNIQUE;

-- Backfill any communities that don't yet have an invite code
UPDATE community
SET invite_code = UPPER(SUBSTRING(MD5(RANDOM()::TEXT), 1, 8))
WHERE invite_code IS NULL;
