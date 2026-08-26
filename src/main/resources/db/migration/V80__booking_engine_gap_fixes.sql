-- V80: Fill booking-engine schema gaps
-- C-1: Three entity columns mapped in Java but absent from V72/V73 migrations.
--       Without these columns, Hibernate validate mode (prod) throws SchemaManagementException at startup.
-- L-2: Persist token number directly on the registration row so it survives reservation purge.
-- M-2: Scope idempotency uniqueness to (key, schedule) so the same UUID cannot lock out a different slot on retry.

-- ── C-1a: event_pooja_schedule ──────────────────────────────────────────────
ALTER TABLE event_pooja_schedule
    ADD COLUMN IF NOT EXISTS notes              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS time_slot_config_id BIGINT;

-- ── C-1b + L-2: event_pooja_user_registrations ──────────────────────────────
ALTER TABLE event_pooja_user_registrations
    ADD COLUMN IF NOT EXISTS pooja_seva_time_slots_id BIGINT,
    ADD COLUMN IF NOT EXISTS token_number             INTEGER;

-- ── M-2: Scope idempotency key to (key, schedule_id) ─────────────────────────
-- The original V72 constraint was table-global; drop it from the renamed table.
ALTER TABLE event_pooja_slot_reservation
    DROP CONSTRAINT IF EXISTS uq_psr_idempotency;

ALTER TABLE event_pooja_slot_reservation
    ADD CONSTRAINT IF NOT EXISTS uq_psr_idempotency_schedule
        UNIQUE (idempotency_key, schedule_id);
