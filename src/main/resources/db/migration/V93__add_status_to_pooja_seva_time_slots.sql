-- Admin-visible status for a config-level time slot.
-- OPEN   → available for booking (default)
-- BLOCKED → admin has temporarily disabled this slot (no new bookings; existing ones unaffected)
-- CLOSED  → permanently closed; will not be included when auto-creating schedule rows
ALTER TABLE event_pooja_seva_time_slots
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
