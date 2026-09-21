-- Apply the renames from V101 that never ran in this environment.
-- For each table: if old exists and new doesn't, rename; if both exist, drop old (Hibernate already created new).
-- player_category was already handled by V118.

DO $$
DECLARE
    pair RECORD;
BEGIN
    FOR pair IN
        SELECT unnest(ARRAY[
            'tournament','tournament_announcement','tournament_gallery_image',
            'tournament_timeline_entry','tournament_contact','tournament_config',
            'tournament_group','tournament_match',
            'auction_config','auction_config_category','auction_team',
            'auction_player','auction_bid','auction_dispute_committee','auction_session_log',
            'group_team_standing','schedule_generation_log',
            'match_result','match_innings','batting_performance','bowling_performance',
            'match_ball_event','player_tournament_stats',
            'court','event_category'
        ]) AS old_name,
        unnest(ARRAY[
            'sports_tournament','sports_tournament_announcement','sports_tournament_gallery_image',
            'sports_tournament_timeline_entry','sports_tournament_contact','sports_tournament_config',
            'sports_tournament_group','sports_tournament_match',
            'sports_auction_config','sports_auction_config_category','sports_auction_team',
            'sports_auction_player','sports_auction_bid','sports_auction_dispute_committee','sports_auction_session_log',
            'sports_group_team_standing','sports_schedule_generation_log',
            'sports_match_result','sports_match_innings','sports_batting_performance','sports_bowling_performance',
            'sports_match_ball_event','sports_player_tournament_stats',
            'sports_court','sports_event_category'
        ]) AS new_name
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'manacommunity' AND table_name = pair.old_name)
        THEN
            IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                           WHERE table_schema = 'manacommunity' AND table_name = pair.new_name)
            THEN
                EXECUTE format('ALTER TABLE manacommunity.%I RENAME TO %I', pair.old_name, pair.new_name);
                RAISE NOTICE 'Renamed % to %', pair.old_name, pair.new_name;
            ELSE
                EXECUTE format('DROP TABLE IF EXISTS manacommunity.%I CASCADE', pair.old_name);
                RAISE NOTICE 'Dropped old table % (new % already exists)', pair.old_name, pair.new_name;
            END IF;
        END IF;
    END LOOP;
END $$;

-- Fix the sports_event -> tournament FK
ALTER TABLE manacommunity.sports_event
    DROP CONSTRAINT IF EXISTS fk_sports_event_tournament;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'manacommunity' AND table_name = 'sports_tournament')
    THEN
        ALTER TABLE manacommunity.sports_event
            ADD CONSTRAINT fk_sports_event_tournament
            FOREIGN KEY (tournament_id) REFERENCES manacommunity.sports_tournament(id);
    END IF;
END $$;
