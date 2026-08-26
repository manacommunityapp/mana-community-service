ALTER TABLE event_pooja_sevas ADD COLUMN IF NOT EXISTS needs_registration BOOLEAN DEFAULT TRUE;
ALTER TABLE event_cultural_events ADD COLUMN IF NOT EXISTS needs_registration BOOLEAN DEFAULT TRUE;
ALTER TABLE event_competitions ADD COLUMN IF NOT EXISTS needs_registration BOOLEAN DEFAULT TRUE;
ALTER TABLE event_lunch_dinners ADD COLUMN IF NOT EXISTS needs_registration BOOLEAN DEFAULT TRUE;
