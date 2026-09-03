ALTER TABLE event_donation
    ADD COLUMN IF NOT EXISTS flat_number VARCHAR(50);
