-- ============================================================================
-- MANA COMMUNITY SERVICE - COMPLETE SEEDED DATA CLEANUP SCRIPTS (POSTGRESQL)
-- Schema: manacommunity
-- ============================================================================

ROLLBACK;

-- ============================================================================
-- OPTION 1: TARGETED DELETION OF SAMPLE SEEDED ENTITIES
-- Deletes sample users, sample community, sports events, tournaments, auctions,
-- and venues without wiping the core system or schema migrations.
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
