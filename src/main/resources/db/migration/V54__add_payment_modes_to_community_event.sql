-- Migration V54: Add payment_modes to community_event table

ALTER TABLE community_event ADD COLUMN IF NOT EXISTS payment_modes VARCHAR(255);
