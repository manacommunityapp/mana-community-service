-- Migration V53: Add gotram column to event registration tables

ALTER TABLE event_booking_registrations ADD COLUMN IF NOT EXISTS gotram VARCHAR(100);
ALTER TABLE event_registration ADD COLUMN IF NOT EXISTS gotram VARCHAR(100);
ALTER TABLE community_event_activity_registration ADD COLUMN IF NOT EXISTS gotram VARCHAR(100);
