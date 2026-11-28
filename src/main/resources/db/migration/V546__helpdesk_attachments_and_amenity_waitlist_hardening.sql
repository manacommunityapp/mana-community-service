-- Helpdesk photo attachment URLs and Amenity slot waitlist lookup index

ALTER TABLE helpdesk_tickets
    ADD COLUMN IF NOT EXISTS attachment_urls TEXT;

CREATE INDEX IF NOT EXISTS idx_facility_slots_waitlist_lookup
    ON facility_slots (facility_id, slot_date, status);
