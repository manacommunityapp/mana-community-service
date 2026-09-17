-- ============================================================================
-- auction_teams_sample_data.sql
-- COMMENTED OUT: Sports Auction sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsAuctionSeeder).
-- ============================================================================
/*
-- Seeding auction_team table linked to Annual Summer Cricket Cup
DO $$
DECLARE
    config_id_val INT;
    event_id_val INT;
    user_sunil INT;
    user_ramesh INT;
    user_user1 INT;
    user_vikram INT;
    user_rohit INT;
BEGIN
    SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';
    
    SELECT id INTO user_sunil FROM app_user WHERE email = 'sunil@gmail.com';
    SELECT id INTO user_ramesh FROM app_user WHERE email = 'ramesh@gmail.com';
    SELECT id INTO user_user1 FROM app_user WHERE email = 'user1@gmail.com';
    SELECT id INTO user_vikram FROM app_user WHERE email = 'vikram.singh@gmail.com';
    SELECT id INTO user_rohit FROM app_user WHERE email = 'rohit.verma@gmail.com';

    IF event_id_val IS NOT NULL THEN
        SELECT id INTO config_id_val FROM auction_config WHERE event_id = event_id_val;
        
        IF config_id_val IS NOT NULL THEN
            -- Team 1
            IF user_sunil IS NOT NULL AND NOT EXISTS (SELECT 1 FROM auction_team WHERE config_id = config_id_val AND team_name = 'Team 1') THEN
                INSERT INTO auction_team (config_id, team_name, captain_user_id, owner_user_id, owner_name, color_hex, total_budget, remaining_budget, spent, event_id, captain_nomination, captain_confirmation)
                VALUES (config_id_val, 'Team 1', user_sunil, user_sunil, 'Sunil Kanthala', '', 100000, 100000, 0, event_id_val, TRUE, TRUE);
            END IF;

            -- Team 2
            IF user_ramesh IS NOT NULL AND NOT EXISTS (SELECT 1 FROM auction_team WHERE config_id = config_id_val AND team_name = 'Team 2') THEN
                INSERT INTO auction_team (config_id, team_name, captain_user_id, owner_user_id, owner_name, color_hex, total_budget, remaining_budget, spent, event_id, captain_nomination, captain_confirmation)
                VALUES (config_id_val, 'Team 2', user_ramesh, user_ramesh, 'Ramesh Korlakunta', '', 100000, 100000, 0, event_id_val, TRUE, TRUE);
            END IF;

            -- Team 3
            IF user_user1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM auction_team WHERE config_id = config_id_val AND team_name = 'Team 3') THEN
                INSERT INTO auction_team (config_id, team_name, captain_user_id, owner_user_id, owner_name, color_hex, total_budget, remaining_budget, spent, event_id, captain_nomination, captain_confirmation)
                VALUES (config_id_val, 'Team 3', user_user1, user_user1, 'user1', '', 100000, 100000, 0, event_id_val, TRUE, TRUE);
            END IF;

            -- Team 4
            IF user_vikram IS NOT NULL AND NOT EXISTS (SELECT 1 FROM auction_team WHERE config_id = config_id_val AND team_name = 'Team 4') THEN
                INSERT INTO auction_team (config_id, team_name, captain_user_id, owner_user_id, owner_name, color_hex, total_budget, remaining_budget, spent, event_id, captain_nomination, captain_confirmation)
                VALUES (config_id_val, 'Team 4', user_vikram, user_vikram, 'Vikram Singh', '', 100000, 100000, 0, event_id_val, TRUE, TRUE);
            END IF;

            -- Team 5
            IF user_rohit IS NOT NULL AND NOT EXISTS (SELECT 1 FROM auction_team WHERE config_id = config_id_val AND team_name = 'Team 5') THEN
                INSERT INTO auction_team (config_id, team_name, captain_user_id, owner_user_id, owner_name, color_hex, total_budget, remaining_budget, spent, event_id, captain_nomination, captain_confirmation)
                VALUES (config_id_val, 'Team 5', user_rohit, user_rohit, 'Rohit Verma', '', 100000, 100000, 0, event_id_val, TRUE, TRUE);
            END IF;
        END IF;
    END IF;
END $$;
*/
