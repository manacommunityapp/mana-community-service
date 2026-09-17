-- ============================================================================
-- sports_events_sample_data.sql
-- COMMENTED OUT: Sports Events sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsEventSeeder).
-- ============================================================================
/*
-- Seeding sports_event table with baseline events
-- Binds to dynamic IDs for: Cricket sport, Lakshmi's Emperia community, LE Box Cricket venue, and Ramesh as created_by
DO $$
DECLARE
    sport_id_val INT;
    comm_id_val INT;
    venue_id_val INT;
    user_id_val INT;
    event_id_val INT;
    cat_boys_id INT;
    cat_mens_id INT;
BEGIN
    SELECT id INTO sport_id_val FROM sports_meta WHERE name = 'Cricket';
    SELECT id INTO comm_id_val FROM community WHERE invite_code = 'LE-MY-HYD';
    SELECT id INTO venue_id_val FROM venue WHERE name = 'LE Box Cricket';
    SELECT id INTO user_id_val FROM app_user WHERE email = 'ramesh@gmail.com';
    
    SELECT id INTO cat_boys_id FROM sports_player_category WHERE name = 'Cricket Youth (8 - 18)';
    SELECT id INTO cat_mens_id FROM sports_player_category WHERE name = 'Cricket Men (Above 18)';

    IF sport_id_val IS NOT NULL AND comm_id_val IS NOT NULL AND venue_id_val IS NOT NULL AND user_id_val IS NOT NULL THEN
        -- Insert tournament event if not exists
        IF NOT EXISTS (SELECT 1 FROM sports_event WHERE name = 'Annual Summer Cricket Cup') THEN
            INSERT INTO sports_event (name, min_age, max_age, active, created_at, updated_at, sport_id, community_id, venue_id, created_by, format, tournament_type, event_date_start, event_date_end, registration_date_start, registration_date_end, max_participants, dispute_committee_ids, auction, auction_enabled)
            VALUES ('Annual Summer Cricket Cup', 0, 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, sport_id_val, comm_id_val, venue_id_val, user_id_val, 'TEAM', 'KNOCKOUT', '2026-09-25', '2026-09-27', '2026-09-16', '2026-09-20', 100, '3,4', TRUE, TRUE);
        END IF;

        -- Get the sports event ID
        SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';

        -- Map categories in the join table
        IF event_id_val IS NOT NULL THEN
            IF cat_boys_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_category WHERE event_id = event_id_val AND category_id = cat_boys_id) THEN
                INSERT INTO sports_event_category (event_id, category_id) VALUES (event_id_val, cat_boys_id);
            END IF;

            IF cat_mens_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_category WHERE event_id = event_id_val AND category_id = cat_mens_id) THEN
                INSERT INTO sports_event_category (event_id, category_id) VALUES (event_id_val, cat_mens_id);
            END IF;
        END IF;
    END IF;
END $$;
*/
