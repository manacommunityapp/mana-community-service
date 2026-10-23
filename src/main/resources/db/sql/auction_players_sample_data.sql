-- ============================================================================
-- auction_players_sample_data.sql
-- COMMENTED OUT: Sports Auction sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsAuctionSeeder).
-- ============================================================================
/*
-- Seeding auction_player table with players based on confirmed tournament registrations
DO $$
DECLARE
    config_id_val INT;
    event_id_val INT;
    reg_record RECORD;
    q_order INT := 1;
BEGIN
    SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';
    
    IF event_id_val IS NOT NULL THEN
        SELECT id INTO config_id_val FROM auction_config WHERE event_id = event_id_val;
        
        IF config_id_val IS NOT NULL THEN
            -- Loop through confirmed event registrations and seed auction players
            FOR reg_record IN 
                SELECT user_id, player_name, role, age 
                FROM sports_event_registration 
                WHERE event_id = event_id_val AND status = 'CONFIRMED'
            LOOP
                IF NOT EXISTS (SELECT 1 FROM auction_player WHERE config_id = config_id_val AND user_id = reg_record.user_id) THEN
                    INSERT INTO auction_player (config_id, user_id, player_name, category, player_role, age, base_price, stats_json, queue_order, status, uploaded_at)
                    VALUES (
                        config_id_val, 
                        reg_record.user_id, 
                        reg_record.player_name, 
                        CASE 
                            WHEN LOWER(reg_record.role) = 'bowler' THEN 'Bowler'
                            WHEN LOWER(reg_record.role) = 'all-rounder' THEN 'All-rounder'
                            WHEN LOWER(reg_record.role) = 'wicket keeper' THEN 'Wicket Keeper'
                            ELSE 'Batsmen'
                        END, 
                        reg_record.role, 
                        reg_record.age, 
                        1000, 
                        '{"matches":24,"runs":620,"wickets":18}', 
                        q_order, 
                        'QUEUED', 
                        CURRENT_TIMESTAMP
                    );
                    q_order := q_order + 1;
                END IF;
            END LOOP;
        END IF;
    END IF;
END $$;
*/
