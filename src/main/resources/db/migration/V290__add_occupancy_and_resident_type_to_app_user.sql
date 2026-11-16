-- V90__add_occupancy_and_resident_type_to_app_user.sql
-- Ensure occupancy_status and resident_type columns exist on app_user table
ALTER TABLE manacommunity.app_user
    ADD COLUMN IF NOT EXISTS occupancy_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS resident_type VARCHAR(30);
