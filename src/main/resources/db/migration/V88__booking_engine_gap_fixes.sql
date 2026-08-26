-- V88: Fill booking-engine schema gaps (C-1, L-2, M-2)
-- C-1: Three entity columns mapped in Java but absent from previous migrations.
--       Without these, Hibernate validate mode (prod) throws SchemaManagementException at startup.
-- L-2: Persist token number on the registration row so it survives reservation purge.
-- M-2: Scope idempotency uniqueness to (key, schedule) so a retried key can't lock out a different slot.

-- ── C-1a: event_pooja_schedule ──────────────────────────────────────────────
ALTER TABLE event_pooja_schedule
    ADD COLUMN IF NOT EXISTS notes               VARCHAR(500),
    ADD COLUMN IF NOT EXISTS time_slot_config_id BIGINT;

-- ── C-1b + L-2: event_pooja_user_registrations ──────────────────────────────
ALTER TABLE event_pooja_user_registrations
    ADD COLUMN IF NOT EXISTS pooja_seva_time_slots_id BIGINT,
    ADD COLUMN IF NOT EXISTS token_number             INTEGER;

-- ── M-2: Scope idempotency key to (key, schedule_id) ─────────────────────────
ALTER TABLE event_pooja_slot_reservation
    DROP CONSTRAINT IF EXISTS uq_psr_idempotency;

ALTER TABLE event_pooja_slot_reservation
    ADD CONSTRAINT IF NOT EXISTS uq_psr_idempotency_schedule
        UNIQUE (idempotency_key, schedule_id);
