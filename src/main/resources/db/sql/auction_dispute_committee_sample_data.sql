-- ============================================================================
-- auction_dispute_committee_sample_data.sql
-- COMMENTED OUT: Sports Auction sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsAuctionSeeder).
-- ============================================================================
/*
-- Seeding auction_dispute_committee table linked to Annual Summer Cricket Cup
DO $$
DECLARE
    config_id_val INT;
    event_id_val INT;
BEGIN
    SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';
    
    IF event_id_val IS NOT NULL THEN
        SELECT id INTO config_id_val FROM auction_config WHERE event_id = event_id_val;
        
        IF config_id_val IS NOT NULL THEN
            IF NOT EXISTS (SELECT 1 FROM auction_dispute_committee WHERE config_id = config_id_val AND member_name = 'Sunil Kanthala') THEN
                INSERT INTO auction_dispute_committee (config_id, member_name, role)
                VALUES (config_id_val, 'Sunil Kanthala', 'COMMITTEE_MEMBER');
            END IF;
        END IF;
    END IF;
END $$;
*/
