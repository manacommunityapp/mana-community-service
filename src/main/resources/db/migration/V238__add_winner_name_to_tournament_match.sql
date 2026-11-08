-- V238: Add winner_name and winner_side to sports_tournament_match for non-auction competitors
ALTER TABLE sports_tournament_match ADD COLUMN IF NOT EXISTS winner_name VARCHAR(255);
ALTER TABLE sports_tournament_match ADD COLUMN IF NOT EXISTS winner_side VARCHAR(10);

