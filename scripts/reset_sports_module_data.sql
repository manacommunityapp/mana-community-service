-- ============================================================================
-- MANA COMMUNITY SERVICE - SPORTS MODULE DATA RESET & DELETION SCRIPTS (POSTGRESQL)
-- Schema: manacommunity
-- ============================================================================

ROLLBACK;

-- ============================================================================
-- OPTION 1: TARGETED CLEANUP OF SEEDED SAMPLE DATA ONLY
-- (Safely deletes only sample records created by Java seeders without wiping user data)
-- ============================================================================
DO $$
DECLARE
    sample_tournament_names text[] := ARRAY[
        'LE 2026 Season Fest',
        'LE 2026 Summer Champ',
        'LE 2026 Winter Cup'
    ];
    sample_event_names text[] := ARRAY[
        'Annual 2026 Cricket Cup',
        'Annual Summer Cricket Cup',
        'Badminton - Singles & Doubles',
        'Badminton Community Championship',
        'Badminton — Men''s Above 19',
        'Chess Championship',
        'Community Chess Championship',
        'Carroms - Singles and Doubles',
        'Carroms Community Cup',
        'Table Tennis - Singles and Doubles',
        'Table Tennis Open Championship',
        'Volleyball Premier League',
        'Community Badminton League 2026'
    ];
    v_event_ids bigint[];
    v_config_ids bigint[];
    v_tournament_ids bigint[];
    has_sports_tournament boolean;
    has_tournament boolean;
BEGIN
    -- Check tournament table name in schema
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament'
    ) INTO has_sports_tournament;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'manacommunity' AND table_name = 'tournament'
    ) INTO has_tournament;

    -- Collect IDs for sample events
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_event') THEN
        SELECT ARRAY_AGG(id) INTO v_event_ids
        FROM manacommunity.sports_event
        WHERE name = ANY(sample_event_names);
    END IF;

    -- Collect IDs for sample tournament configs
    IF v_event_ids IS NOT NULL AND array_length(v_event_ids, 1) > 0 AND
       EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament_config') THEN
        SELECT ARRAY_AGG(id) INTO v_config_ids
        FROM manacommunity.sports_tournament_config
        WHERE event_id = ANY(v_event_ids);
    END IF;

    -- Collect IDs for sample tournaments
    IF has_sports_tournament THEN
        EXECUTE 'SELECT ARRAY_AGG(id) FROM manacommunity.sports_tournament WHERE name = ANY($1)'
        INTO v_tournament_ids
        USING sample_tournament_names;
    ELSIF has_tournament THEN
        EXECUTE 'SELECT ARRAY_AGG(id) FROM manacommunity.tournament WHERE name = ANY($1)'
        INTO v_tournament_ids
        USING sample_tournament_names;
    END IF;

    RAISE NOTICE 'Found sample events: %, configs: %, tournaments: %', 
        COALESCE(array_length(v_event_ids, 1), 0),
        COALESCE(array_length(v_config_ids, 1), 0),
        COALESCE(array_length(v_tournament_ids, 1), 0);

    -- 1. Delete Auction Data linked to sample events
    IF v_event_ids IS NOT NULL AND array_length(v_event_ids, 1) > 0 THEN
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_bid') THEN
            DELETE FROM manacommunity.sports_auction_bid 
            WHERE config_id IN (SELECT id FROM manacommunity.sports_auction_config WHERE event_id = ANY(v_event_ids));
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_session_log') THEN
            DELETE FROM manacommunity.sports_auction_session_log 
            WHERE config_id IN (SELECT id FROM manacommunity.sports_auction_config WHERE event_id = ANY(v_event_ids));
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_player') THEN
            DELETE FROM manacommunity.sports_auction_player 
            WHERE config_id IN (SELECT id FROM manacommunity.sports_auction_config WHERE event_id = ANY(v_event_ids));
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_team') THEN
            DELETE FROM manacommunity.sports_auction_team 
            WHERE config_id IN (SELECT id FROM manacommunity.sports_auction_config WHERE event_id = ANY(v_event_ids))
               OR event_id = ANY(v_event_ids);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_dispute_committee') THEN
            DELETE FROM manacommunity.sports_auction_dispute_committee 
            WHERE config_id IN (SELECT id FROM manacommunity.sports_auction_config WHERE event_id = ANY(v_event_ids));
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_config_category') THEN
            DELETE FROM manacommunity.sports_auction_config_category 
            WHERE config_id IN (SELECT id FROM manacommunity.sports_auction_config WHERE event_id = ANY(v_event_ids));
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_auction_config') THEN
            DELETE FROM manacommunity.sports_auction_config 
            WHERE event_id = ANY(v_event_ids);
        END IF;
    END IF;

    -- 2. Delete Tournament Scheduler Matches, Stats & Scoring Data
    IF v_config_ids IS NOT NULL AND array_length(v_config_ids, 1) > 0 THEN
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_batting_performance') THEN
            DELETE FROM manacommunity.sports_batting_performance WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_bowling_performance') THEN
            DELETE FROM manacommunity.sports_bowling_performance WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_player_match_stats') THEN
            DELETE FROM manacommunity.sports_player_match_stats WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_player_tournament_stats') THEN
            DELETE FROM manacommunity.sports_player_tournament_stats WHERE tournament_config_id = ANY(v_config_ids);
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_match_ball_event') THEN
            DELETE FROM manacommunity.sports_match_ball_event WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_match_event') THEN
            DELETE FROM manacommunity.sports_match_event WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_match_innings') THEN
            DELETE FROM manacommunity.sports_match_innings WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_match_period_score') THEN
            DELETE FROM manacommunity.sports_match_period_score WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_match_result') THEN
            DELETE FROM manacommunity.sports_match_result WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_race_result') THEN
            DELETE FROM manacommunity.sports_race_result WHERE match_id IN (SELECT id FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_group_team_standing') THEN
            DELETE FROM manacommunity.sports_group_team_standing WHERE group_id IN (SELECT id FROM manacommunity.sports_tournament_group WHERE tournament_config_id = ANY(v_config_ids));
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament_match') THEN
            DELETE FROM manacommunity.sports_tournament_match WHERE tournament_config_id = ANY(v_config_ids);
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament_group') THEN
            DELETE FROM manacommunity.sports_tournament_group WHERE tournament_config_id = ANY(v_config_ids);
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_scoring_config') THEN
            DELETE FROM manacommunity.sports_scoring_config WHERE tournament_config_id = ANY(v_config_ids);
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_schedule_generation_log') THEN
            DELETE FROM manacommunity.sports_schedule_generation_log WHERE tournament_config_id = ANY(v_config_ids);
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament_config') THEN
            DELETE FROM manacommunity.sports_tournament_config WHERE id = ANY(v_config_ids);
        END IF;
    END IF;

    -- 3. Delete Event Registrations, Notifications & Join Table References
    IF v_event_ids IS NOT NULL AND array_length(v_event_ids, 1) > 0 THEN
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_event_registration') THEN
            DELETE FROM manacommunity.sports_event_registration WHERE event_id = ANY(v_event_ids);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_scheduled_notifications') THEN
            DELETE FROM manacommunity.sports_scheduled_notifications WHERE event_id = ANY(v_event_ids);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_event_category') THEN
            DELETE FROM manacommunity.sports_event_category WHERE event_id = ANY(v_event_ids);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_event_dispute_committee') THEN
            DELETE FROM manacommunity.sports_event_dispute_committee WHERE sports_event_id = ANY(v_event_ids);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_event_contact') THEN
            DELETE FROM manacommunity.sports_event_contact WHERE sports_event_id = ANY(v_event_ids);
        END IF;

        -- Unlink tournament from events before deleting events
        UPDATE manacommunity.sports_event SET tournament_id = NULL WHERE id = ANY(v_event_ids);
        DELETE FROM manacommunity.sports_event WHERE id = ANY(v_event_ids);
    END IF;

    -- 4. Delete Tournaments
    IF v_tournament_ids IS NOT NULL AND array_length(v_tournament_ids, 1) > 0 THEN
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament_contact') THEN
            DELETE FROM manacommunity.sports_tournament_contact WHERE tournament_id = ANY(v_tournament_ids);
        END IF;

        IF has_sports_tournament THEN
            EXECUTE 'DELETE FROM manacommunity.sports_tournament WHERE id = ANY($1)' USING v_tournament_ids;
        ELSIF has_tournament THEN
            EXECUTE 'DELETE FROM manacommunity.tournament WHERE id = ANY($1)' USING v_tournament_ids;
        END IF;
    END IF;

    RAISE NOTICE 'Targeted sports sample data successfully cleaned!';
END $$;


-- ============================================================================
-- OPTION 2: COMPLETE SPORTS MODULE TRUNCATE (SAFE DYNAMIC EXECUTION)
-- Checks table existence before truncating each table in the schema.
-- ============================================================================
DO $$
DECLARE
    tbl text;
    tables_to_clear text[] := ARRAY[
        'sports_auction_bid',
        'sports_auction_session_log',
        'sports_auction_player',
        'sports_auction_team',
        'sports_auction_dispute_committee',
        'sports_auction_config_category',
        'sports_auction_config',
        'sports_batting_performance',
        'sports_bowling_performance',
        'sports_player_match_stats',
        'sports_player_tournament_stats',
        'sports_match_ball_event',
        'sports_match_event',
        'sports_match_innings',
        'sports_match_period_score',
        'sports_match_result',
        'sports_race_result',
        'sports_group_team_standing',
        'sports_tournament_match',
        'sports_tournament_group',
        'sports_scoring_config',
        'sports_schedule_generation_log',
        'sports_tournament_config',
        'sports_player_ranking',
        'sports_scheduled_notifications',
        'sports_event_registration',
        'sports_event_category',
        'sports_event_dispute_committee',
        'sports_event_contact',
        'sports_tournament_contact',
        'sports_event',
        'sports_tournament',
        'tournament',
        'sports_court'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables_to_clear
    LOOP
        IF EXISTS (
            SELECT 1 
            FROM information_schema.tables 
            WHERE table_schema = 'manacommunity' AND table_name = tbl
        ) THEN
            EXECUTE 'TRUNCATE TABLE manacommunity.' || quote_ident(tbl) || ' RESTART IDENTITY CASCADE';
            RAISE NOTICE 'Successfully cleared: manacommunity.%', tbl;
        END IF;
    END LOOP;
END $$;
