-- V73: Link pooja registrations to the new schedule entity
-- and extend event_pooja_sevas with booking-engine config fields.

-- ── event_pooja_user_registrations: add schedule + reservation FK columns ──
ALTER TABLE event_pooja_user_registrations
    ADD COLUMN IF NOT EXISTS schedule_id    BIGINT,
    ADD COLUMN IF NOT EXISTS reservation_id BIGINT;

ALTER TABLE event_pooja_user_registrations
    ADD CONSTRAINT IF NOT EXISTS fk_eppur_schedule
        FOREIGN KEY (schedule_id) REFERENCES pooja_schedule(id);

CREATE INDEX IF NOT EXISTS idx_eppur_schedule    ON event_pooja_user_registrations(schedule_id);
CREATE INDEX IF NOT EXISTS idx_eppur_reservation ON event_pooja_user_registrations(reservation_id);

-- ── event_pooja_sevas: booking-engine configuration fields ──
ALTER TABLE event_pooja_sevas
    ADD COLUMN IF NOT EXISTS booking_open              TIMESTAMP,
    ADD COLUMN IF NOT EXISTS booking_close             TIMESTAMP,
    ADD COLUMN IF NOT EXISTS max_devotees_per_booking  INTEGER DEFAULT 6,
    ADD COLUMN IF NOT EXISTS prasadam_available        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sankalpam_required        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS approval_required         BOOLEAN NOT NULL DEFAULT FALSE;
