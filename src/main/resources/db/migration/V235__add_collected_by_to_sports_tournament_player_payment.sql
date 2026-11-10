-- ──────────────────────────────────────────────────────────────────────────────
-- V235: Add payment collector attribution columns to sports_tournament_player_payment
-- ──────────────────────────────────────────────────────────────────────────────

ALTER TABLE sports_tournament_player_payment
    ADD COLUMN IF NOT EXISTS collected_by_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS collected_by_contact VARCHAR(150);