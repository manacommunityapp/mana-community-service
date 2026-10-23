-- V185: Add resolution fields to commerce_dispute for Phase 15 admin resolve flow

ALTER TABLE commerce_dispute
    ADD COLUMN IF NOT EXISTS resolution       VARCHAR(64),
    ADD COLUMN IF NOT EXISTS admin_note       TEXT,
    ADD COLUMN IF NOT EXISTS resolved_by_id   BIGINT REFERENCES app_user(id) ON DELETE SET NULL;
