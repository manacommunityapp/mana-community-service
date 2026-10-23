-- V156__add_cricheroes_to_sports_event_registration.sql
-- Add CricHeroes fields to sports_event_registration table

ALTER TABLE sports_event_registration ADD COLUMN IF NOT EXISTS cricheroes_url VARCHAR(500);
ALTER TABLE sports_event_registration ADD COLUMN IF NOT EXISTS cricheroes_id VARCHAR(100);
ALTER TABLE sports_event_registration ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_sports_event_reg_ch_id ON sports_event_registration(cricheroes_id);
