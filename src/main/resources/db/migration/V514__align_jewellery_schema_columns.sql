-- =============================================================================
-- V514__align_jewellery_schema_columns.sql
-- Align jewellery module PostgreSQL column lengths and constraints with JPA entities
-- (preserving V310 checksum for environments where V310 is already applied):
-- 1. jewellery_vendor_profile: widen phone_number (20), pan_number (20), verification_status (30)
-- 2. jewellery_product: widen jewellery_type (60), huid (16), status (30)
-- 3. jewellery_product_asset: widen asset_type (40), body_part (30)
-- 4. jewellery_tryon_session: widen status (30)
-- 5. jewellery_photo_job: allow source_object_key to be NULL after DPDP 24h erasure
-- 6. jewellery_appointment: widen appointment_time (20)
-- =============================================================================

ALTER TABLE IF EXISTS jewellery_vendor_profile
    ALTER COLUMN phone_number TYPE VARCHAR(20),
    ALTER COLUMN pan_number TYPE VARCHAR(20),
    ALTER COLUMN verification_status TYPE VARCHAR(30);

ALTER TABLE IF EXISTS jewellery_product
    ALTER COLUMN jewellery_type TYPE VARCHAR(60),
    ALTER COLUMN huid TYPE VARCHAR(16),
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE IF EXISTS jewellery_product_asset
    ALTER COLUMN asset_type TYPE VARCHAR(40),
    ALTER COLUMN body_part TYPE VARCHAR(30);

ALTER TABLE IF EXISTS jewellery_tryon_session
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE IF EXISTS jewellery_photo_job
    ALTER COLUMN source_object_key DROP NOT NULL;

ALTER TABLE IF EXISTS jewellery_appointment
    ALTER COLUMN appointment_time TYPE VARCHAR(20);
