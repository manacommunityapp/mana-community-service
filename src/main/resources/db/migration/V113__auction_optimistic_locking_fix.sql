-- Fix: V15_auction_optimistic_locking.sql targeted non-existent table names
-- (auction_config / auction_player / auction_team). Adding version columns to
-- the correct manacommunity-schema tables used by the JPA entities.
ALTER TABLE manacommunity.sports_auction_config ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE manacommunity.sports_auction_player ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE manacommunity.sports_auction_team   ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
