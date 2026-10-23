-- Add innings, best bowling, and verified_at columns to sports_auction_player for CricHeroes integration
ALTER TABLE sports_auction_player ADD COLUMN IF NOT EXISTS innings INTEGER;
ALTER TABLE sports_auction_player ADD COLUMN IF NOT EXISTS best_bowling VARCHAR(20);
ALTER TABLE sports_auction_player ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
