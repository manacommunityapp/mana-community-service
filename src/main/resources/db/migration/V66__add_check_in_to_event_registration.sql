-- V66: Add check-in status and timestamp to event registrations
ALTER TABLE event_registration ADD COLUMN IF NOT EXISTS checked_in BOOLEAN DEFAULT FALSE;
ALTER TABLE event_registration ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;

ALTER TABLE event_booking_registrations ADD COLUMN IF NOT EXISTS checked_in BOOLEAN DEFAULT FALSE;
ALTER TABLE event_booking_registrations ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;