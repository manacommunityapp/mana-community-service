-- =====================================================================
-- V126: Sports module — length constraints, tournament name NOT NULL,
--       SportsMeta per-community unique constraint
-- =====================================================================

-- ── 1. Venue ─────────────────────────────────────────────────────────
ALTER TABLE manacommunity.venue ALTER COLUMN name TYPE varchar(150);
ALTER TABLE manacommunity.venue ALTER COLUMN address TYPE varchar(500);
ALTER TABLE manacommunity.venue ALTER COLUMN city TYPE varchar(100);
ALTER TABLE manacommunity.venue ALTER COLUMN area TYPE varchar(100);
ALTER TABLE manacommunity.venue ALTER COLUMN pin_code TYPE varchar(10);
ALTER TABLE manacommunity.venue ALTER COLUMN map_link TYPE varchar(1000);
ALTER TABLE manacommunity.venue ALTER COLUMN venue_type TYPE varchar(30);
ALTER TABLE manacommunity.venue ALTER COLUMN venue_category TYPE varchar(50);
ALTER TABLE manacommunity.venue ALTER COLUMN opening_time TYPE varchar(20);
ALTER TABLE manacommunity.venue ALTER COLUMN closing_time TYPE varchar(20);
ALTER TABLE manacommunity.venue ALTER COLUMN contact_name TYPE varchar(100);
ALTER TABLE manacommunity.venue ALTER COLUMN contact_number TYPE varchar(20);
ALTER TABLE manacommunity.venue ALTER COLUMN contact_email TYPE varchar(150);
ALTER TABLE manacommunity.venue ALTER COLUMN contact_title TYPE varchar(100);

-- ── 2. SportsTournament ──────────────────────────────────────────────
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN name SET NOT NULL;
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN name TYPE varchar(150);
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN start_time TYPE varchar(20);
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN due_time TYPE varchar(20);
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN contact_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN contact_number TYPE varchar(20);
ALTER TABLE manacommunity.sports_tournament ALTER COLUMN contact_email TYPE varchar(150);

-- ── 3. SportsEvent ───────────────────────────────────────────────────
ALTER TABLE manacommunity.sports_event ALTER COLUMN start_time TYPE varchar(20);
ALTER TABLE manacommunity.sports_event ALTER COLUMN due_time TYPE varchar(20);
ALTER TABLE manacommunity.sports_event ALTER COLUMN contact_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_event ALTER COLUMN contact_number TYPE varchar(20);
ALTER TABLE manacommunity.sports_event ALTER COLUMN contact_email TYPE varchar(150);

-- ── 4. SportsAuctionTeam ─────────────────────────────────────────────
ALTER TABLE manacommunity.sports_auction_team ALTER COLUMN team_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_auction_team ALTER COLUMN owner_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_auction_team ALTER COLUMN color_hex TYPE varchar(10);

-- ── 5. SportsAuctionPlayer ───────────────────────────────────────────
ALTER TABLE manacommunity.sports_auction_player ALTER COLUMN player_name TYPE varchar(150);
ALTER TABLE manacommunity.sports_auction_player ALTER COLUMN category TYPE varchar(50);
ALTER TABLE manacommunity.sports_auction_player ALTER COLUMN player_role TYPE varchar(50);

-- ── 6. SportsAuctionConfig ───────────────────────────────────────────
ALTER TABLE manacommunity.sports_auction_config ALTER COLUMN season_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_auction_config ALTER COLUMN auction_format TYPE varchar(30);
ALTER TABLE manacommunity.sports_auction_config ALTER COLUMN unsold_rule TYPE varchar(30);
ALTER TABLE manacommunity.sports_auction_config ALTER COLUMN status TYPE varchar(20);

-- ── 7. SportsAuctionSessionLog ───────────────────────────────────────
ALTER TABLE manacommunity.sports_auction_session_log ALTER COLUMN action TYPE varchar(50);
ALTER TABLE manacommunity.sports_auction_session_log ALTER COLUMN notes TYPE varchar(2000);

-- ── 8. SportsAuctionDisputeCommittee ─────────────────────────────────
ALTER TABLE manacommunity.sports_auction_dispute_committee ALTER COLUMN member_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_auction_dispute_committee ALTER COLUMN role TYPE varchar(30);

-- ── 9. SportsAuctionConfigCategory ───────────────────────────────────
ALTER TABLE manacommunity.sports_auction_config_category ALTER COLUMN category_name TYPE varchar(100);

-- ── 10. SportsCourt ──────────────────────────────────────────────────
ALTER TABLE manacommunity.sports_court ALTER COLUMN name TYPE varchar(100);
ALTER TABLE manacommunity.sports_court ALTER COLUMN opening_time TYPE varchar(20);
ALTER TABLE manacommunity.sports_court ALTER COLUMN closing_time TYPE varchar(20);

-- ── 11. SportsEventRegistration ──────────────────────────────────────
ALTER TABLE manacommunity.sports_event_registration ALTER COLUMN email TYPE varchar(150);
ALTER TABLE manacommunity.sports_event_registration ALTER COLUMN relation TYPE varchar(50);
ALTER TABLE manacommunity.sports_event_registration ALTER COLUMN flat_number TYPE varchar(30);
ALTER TABLE manacommunity.sports_event_registration ALTER COLUMN proposed_team_name TYPE varchar(100);
ALTER TABLE manacommunity.sports_event_registration ALTER COLUMN match_type TYPE varchar(20);
ALTER TABLE manacommunity.sports_event_registration ALTER COLUMN status TYPE varchar(20);

-- ── 12. SportsMeta — per-community unique instead of global ──────────
-- Drop the global unique constraint on name (may be a constraint or index)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conrelid = 'manacommunity.sports_meta'::regclass
               AND conname = 'sports_meta_name_key') THEN
        ALTER TABLE manacommunity.sports_meta DROP CONSTRAINT sports_meta_name_key;
    END IF;
END$$;
DROP INDEX IF EXISTS manacommunity.sports_meta_name_key;

-- Create per-community unique (NULL community_id treated as shared/global via COALESCE)
CREATE UNIQUE INDEX IF NOT EXISTS uq_sports_meta_name_community
    ON manacommunity.sports_meta (LOWER(name), COALESCE(community_id, 0));
