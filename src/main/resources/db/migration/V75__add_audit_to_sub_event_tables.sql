-- V75: Add full audit columns (created_at/updated_at/created_by/updated_by) to
-- sub-event tables that previously tracked only created_at.
--
-- Columns are nullable so existing rows are unaffected.
-- Spring Data @CreatedBy / @LastModifiedBy populates the actor columns on new
-- writes; @PrePersist / @PreUpdate on BaseAuditEntity handles the timestamps.
--
-- Tables:
--   event_competitions        — adds updated_at, created_by, updated_by
--   event_cultural_events     — adds updated_at, created_by, updated_by
--   event_lunch_dinners       — adds updated_at, created_by, updated_by
--   event_ticket_categories   — adds created_by, updated_by (already has timestamps)
--   event_registration        — adds updated_at, created_by, updated_by

-- ── event_competitions ───────────────────────────────────────────────────────
ALTER TABLE event_competitions
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0;

-- ── event_cultural_events ────────────────────────────────────────────────────
ALTER TABLE event_cultural_events
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0;

-- ── event_lunch_dinners ──────────────────────────────────────────────────────
ALTER TABLE event_lunch_dinners
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0;

-- ── event_ticket_categories ──────────────────────────────────────────────────
ALTER TABLE event_ticket_categories
    ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0;

-- ── event_registration ───────────────────────────────────────────────────────
ALTER TABLE event_registration
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0;
