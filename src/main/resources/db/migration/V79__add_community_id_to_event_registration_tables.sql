ALTER TABLE event_registration ADD COLUMN IF NOT EXISTS community_id BIGINT;
ALTER TABLE event_meal_registrations ADD COLUMN IF NOT EXISTS community_id BIGINT;
ALTER TABLE event_activity_registrations ADD COLUMN IF NOT EXISTS community_id BIGINT;
ALTER TABLE event_pooja_schedule ADD COLUMN IF NOT EXISTS community_id BIGINT;
ALTER TABLE event_pooja_slot_reservation ADD COLUMN IF NOT EXISTS community_id BIGINT;
