-- Add registration deadline to cultural sub-events (mirrors the reg_deadline already on event_competitions).
-- Allows admins to close registration before the event date.

ALTER TABLE event_cultural_events
    ADD COLUMN IF NOT EXISTS reg_deadline DATE;
