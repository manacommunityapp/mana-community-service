-- ============================================================================
-- auction_config_sample_data.sql
-- COMMENTED OUT: Sports Auction sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsAuctionSeeder).
-- ============================================================================
/*
-- Seeding auction_config table with tournament auction configurations
-- Binds dynamically to Cricket sport, Annual Summer Cricket Cup event, and Super Admin as creator
DO $$
DECLARE
    sport_id_val INT;
    event_id_val INT;
    user_id_val INT;
BEGIN
    SELECT id INTO sport_id_val FROM sports_meta WHERE name = 'Cricket';
    SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';
    SELECT id INTO user_id_val FROM app_user WHERE email = 'admin@manacommunity.com';

    IF sport_id_val IS NOT NULL AND event_id_val IS NOT NULL AND user_id_val IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM auction_config WHERE event_id = event_id_val) THEN
            INSERT INTO auction_config (sport_id, event_id, season_name, auction_format, total_teams, total_players, budget_per_team, base_price, bid_increment_default, bid_increment_threshold, bid_increment_above, bid_timer_seconds, rtm_enabled, unsold_rule, status, created_by)
            VALUES (sport_id_val, event_id_val, 'Season 2026', 'OPEN_AUCTION', 6, 30, 100000, 1000, 1000, 10000, 5000, 30, TRUE, 'ROTATION_AUCTION', 'DRAFT', user_id_val);
        END IF;
    END IF;
END $$;
*/
