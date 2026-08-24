-- V74: Add created_by / updated_by audit columns to event management tables
--
-- All columns are nullable so existing rows are unaffected and no backfill
-- is required. The application layer (Spring Data @CreatedBy / @LastModifiedBy)
-- populates them on every new insert / update going forward.
--
-- Tables covered:
--   event_booking_registrations   — main unified booking table
--   event_pooja_user_registrations — dedicated pooja registration table
--   pooja_schedule                 — admin-managed booking slot definitions
--   pooja_slot_reservation         — pre-hold capacity reservations (TTL-based)
--   event_pooja_sevas              — pooja seva definitions (also gains updated_at)
--   community_event                — top-level event (already has created_by FK; adds updated_by)

-- ── event_booking_registrations ─────────────────────────────────────────────
ALTER TABLE event_booking_registrations
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_ebr_created_by ON event_booking_registrations(created_by);

-- ── event_pooja_user_registrations ──────────────────────────────────────────
ALTER TABLE event_pooja_user_registrations
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_epur_created_by ON event_pooja_user_registrations(created_by);

-- ── pooja_schedule ──────────────────────────────────────────────────────────
ALTER TABLE pooja_schedule
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ── pooja_slot_reservation ──────────────────────────────────────────────────
ALTER TABLE pooja_slot_reservation
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ── event_pooja_sevas ───────────────────────────────────────────────────────
-- Also adds updated_at which was missing from this table.
ALTER TABLE event_pooja_sevas
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by  BIGINT;

-- ── community_event ─────────────────────────────────────────────────────────
-- created_by is an existing FK column (BIGINT → app_user.id); only updated_by is new.
ALTER TABLE community_event
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;
