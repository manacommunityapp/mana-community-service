-- V71: Add main_event_id column to event_booking_registrations
--
-- Purpose: Link each registration to its parent community (top-level) event.
--          This enables reliable deduplication for pooja seva registrations:
--          if a user has already registered for a Pooja Seva, the backend can
--          detect it by activityId + mainEventId and the frontend can present
--          Update Mode instead of allowing a duplicate fresh booking.
--
-- Safe to run on existing data: column is nullable so old rows are unaffected.

ALTER TABLE event_booking_registrations
    ADD COLUMN IF NOT EXISTS main_event_id BIGINT NULL;

-- Index for future queries: registrations by user within a parent event
CREATE INDEX IF NOT EXISTS idx_ebr_user_main_event
    ON event_booking_registrations (user_id, main_event_id)
    WHERE main_event_id IS NOT NULL;
