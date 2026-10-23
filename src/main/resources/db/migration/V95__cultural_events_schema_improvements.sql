-- Add status lifecycle field to cultural sub-events
ALTER TABLE event_cultural_events
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Add capacity tracking (needed once registration flow is active)
ALTER TABLE event_cultural_events
    ADD COLUMN IF NOT EXISTS capacity INT;

-- Add sort_order so admins can define program schedule sequence
ALTER TABLE event_cultural_events
    ADD COLUMN IF NOT EXISTS sort_order INT;

-- Fix: has_backtrack was defaulting to TRUE; most performances don't use a backing track
ALTER TABLE event_cultural_events
    ALTER COLUMN has_backtrack SET DEFAULT FALSE;
