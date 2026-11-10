-- ============================================================================
-- MANA COMMUNITY SERVICE - COMPLETE SEEDED DATA CLEANUP SCRIPTS (POSTGRESQL)
-- Schema: manacommunity
-- ============================================================================

ROLLBACK;

-- ============================================================================
-- ENVIRONMENT SAFETY GUARD: STRICTLY LOCAL DATABASE ONLY
-- Prohibits execution on AWS Lightsail, AWS RDS, Dev, UAT, and Prod databases.
-- ============================================================================
DO $$
DECLARE
    v_server_addr inet := inet_server_addr();
    v_allow_reset text := current_setting('app.allow_local_reset', true);
BEGIN
    -- 1. STRICT REMOTE SERVER BLOCK:
    -- inet_server_addr() is NULL for local unix socket, or '127.0.0.1' / '::1' for localhost loopback.
    -- If connected over network to any non-loopback IP (e.g. AWS Lightsail 3.109.145.130 or VPC 172.x), ABORT IMMEDIATELY.
    IF v_server_addr IS NOT NULL AND v_server_addr NOT IN ('127.0.0.1'::inet, '::1'::inet) THEN
        RAISE EXCEPTION 'CRITICAL ENVIRONMENT SAFETY ERROR: Destructive reset scripts are strictly prohibited on remote/AWS servers! Detected database server IP: %. Execution ABORTED.', v_server_addr;
    END IF;

    -- 2. EXPLICIT LOCAL CONFIRMATION GUARD:
    -- Prevent accidental execution. Must run `SET app.allow_local_reset = 'true';` first.
    IF v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'SAFETY GUARD: This script will WIPE all seeded data and is restricted to LOCAL environments only. To run on your LOCAL database, you must first execute in this session: SET app.allow_local_reset = ''true'';';
    END IF;
END $$;

-- ============================================================================
-- OPTION 1: TARGETED DELETION OF SAMPLE SEEDED ENTITIES
-- Deletes sample users, sample community, sports events, tournaments, auctions,
-- and venues without wiping the core system or schema migrations.
-- ============================================================================
DO $$
DECLARE
    v_server_addr inet := inet_server_addr();
    v_allow_reset text := current_setting('app.allow_local_reset', true);
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
    -- Strict environment guard
    IF (v_server_addr IS NOT NULL AND v_server_addr NOT IN ('127.0.0.1'::inet, '::1'::inet)) OR v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'CRITICAL BLOCKED: reset_all_seeded_data can ONLY run on a LOCAL database (127.0.0.1) with SET app.allow_local_reset = ''true''; (detected server: %)', v_server_addr;
    END IF;

    -- 1. Safely unlink tournament foreign key on sports_event if table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'manacommunity' AND table_name = 'sports_event') THEN
        EXECUTE 'UPDATE manacommunity.sports_event SET tournament_id = NULL';
    END IF;

    -- 2. Clear all tables dynamically
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

    RAISE NOTICE 'Seeded sample data successfully removed.';
END $$;
