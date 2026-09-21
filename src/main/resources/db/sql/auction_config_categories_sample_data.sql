-- ============================================================================
-- auction_config_categories_sample_data.sql
-- COMMENTED OUT: Sports Auction sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsAuctionSeeder).
-- ============================================================================
/*
-- Seeding auction_config_category table with categories linked to Annual Summer Cricket Cup
DO $$
DECLARE
    config_id_val INT;
    event_id_val INT;
BEGIN
    SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';
    
    IF event_id_val IS NOT NULL THEN
        SELECT id INTO config_id_val FROM auction_config WHERE event_id = event_id_val;
        
        IF config_id_val IS NOT NULL THEN
            IF NOT EXISTS (SELECT 1 FROM auction_config_category WHERE config_id = config_id_val AND category_name = 'Batsmen') THEN
                INSERT INTO auction_config_category (config_id, category_name) VALUES (config_id_val, 'Batsmen');
            END IF;

            IF NOT EXISTS (SELECT 1 FROM auction_config_category WHERE config_id = config_id_val AND category_name = 'Bowler') THEN
                INSERT INTO auction_config_category (config_id, category_name) VALUES (config_id_val, 'Bowler');
            END IF;

            IF NOT EXISTS (SELECT 1 FROM auction_config_category WHERE config_id = config_id_val AND category_name = 'All-rounder') THEN
                INSERT INTO auction_config_category (config_id, category_name) VALUES (config_id_val, 'All-rounder');
            END IF;

            IF NOT EXISTS (SELECT 1 FROM auction_config_category WHERE config_id = config_id_val AND category_name = 'Wicket Keeper') THEN
                INSERT INTO auction_config_category (config_id, category_name) VALUES (config_id_val, 'Wicket Keeper');
            END IF;
        END IF;
    END IF;
END $$;
*/
