-- Migration V58: Add payment details, notes, and contacts to community_event table

ALTER TABLE community_event ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100);
ALTER TABLE community_event ADD COLUMN IF NOT EXISTS scanner_url VARCHAR(500);
ALTER TABLE community_event ADD COLUMN IF NOT EXISTS notes VARCHAR(3000);
ALTER TABLE community_event ADD COLUMN IF NOT EXISTS contacts_json VARCHAR(3000);
ALTER TABLE community_event ADD COLUMN IF NOT EXISTS payment_instructions VARCHAR(2000);
