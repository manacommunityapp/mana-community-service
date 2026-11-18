-- V330: Add voided_at column to sports_tournament_player_payment
-- Physical deletes are replaced by VOIDED status so financial records are immutable.
ALTER TABLE sports_tournament_player_payment
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMPTZ;
