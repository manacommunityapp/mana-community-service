-- Add wizard fields captured by the Eventify-style registration flow
ALTER TABLE event_booking_registrations
    ADD COLUMN IF NOT EXISTS phone       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS email       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ticket_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS notes       TEXT,
    ADD COLUMN IF NOT EXISTS agreed      BOOLEAN NOT NULL DEFAULT FALSE;
