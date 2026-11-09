-- V239: Add captain_nomination_open column to sports_event table
ALTER TABLE sports_event
ADD COLUMN IF NOT EXISTS captain_nomination_open BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN sports_event.captain_nomination_open IS 'Indicates whether captain nominations are currently open for registered players in this event';
