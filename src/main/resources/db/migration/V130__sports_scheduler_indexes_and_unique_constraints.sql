-- =====================================================================
-- V130: Add indexes and unique constraints to sports scheduler/scoring
--       tables. These tables had ZERO indexes beyond the PK, causing
--       full table scans on all FK-based joins and lookups.
--
--       Sections:
--         A. sports_tournament_match  (highest query volume)
--         B. sports_match_ball_event  (highest row volume)
--         C. sports_match_result
--         D. sports_match_innings
--         E. sports_batting_performance
--         F. sports_bowling_performance
--         G. sports_match_event
--         H. sports_match_period_score
--         I. sports_player_match_stats
--         J. sports_race_result
--         K. sports_group_team_standing
--         L. sports_tournament_group
--         M. sports_scoring_config
--         N. sports_tournament_config
-- =====================================================================


-- ══ A. sports_tournament_match ═══════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_tournament_match_config_id
    ON manacommunity.sports_tournament_match(config_id);

CREATE INDEX IF NOT EXISTS idx_tournament_match_config_round
    ON manacommunity.sports_tournament_match(config_id, round_number);

CREATE INDEX IF NOT EXISTS idx_tournament_match_config_group
    ON manacommunity.sports_tournament_match(config_id, group_id);

CREATE INDEX IF NOT EXISTS idx_tournament_match_team_a
    ON manacommunity.sports_tournament_match(team_a_id);

CREATE INDEX IF NOT EXISTS idx_tournament_match_team_b
    ON manacommunity.sports_tournament_match(team_b_id);

CREATE INDEX IF NOT EXISTS idx_tournament_match_scheduled_at
    ON manacommunity.sports_tournament_match(scheduled_at);

CREATE INDEX IF NOT EXISTS idx_tournament_match_status
    ON manacommunity.sports_tournament_match(config_id, status);

CREATE INDEX IF NOT EXISTS idx_tournament_match_community
    ON manacommunity.sports_tournament_match(community_id);

CREATE INDEX IF NOT EXISTS idx_tournament_match_venue
    ON manacommunity.sports_tournament_match(venue_id);


-- ══ B. sports_match_ball_event ═══════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_ball_event_match_id
    ON manacommunity.sports_match_ball_event(match_id);

CREATE INDEX IF NOT EXISTS idx_ball_event_match_over
    ON manacommunity.sports_match_ball_event(match_id, innings_number, over_number);

CREATE INDEX IF NOT EXISTS idx_ball_event_batsman
    ON manacommunity.sports_match_ball_event(batsman_id);

CREATE INDEX IF NOT EXISTS idx_ball_event_bowler
    ON manacommunity.sports_match_ball_event(bowler_id);


-- ══ C. sports_match_result ═══════════════════════════════════════════
-- match_id already has a UNIQUE constraint from OneToOne, but an
-- explicit index speeds up joins from innings/standings.

CREATE INDEX IF NOT EXISTS idx_match_result_match_id
    ON manacommunity.sports_match_result(match_id);


-- ══ D. sports_match_innings ══════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_match_innings_result_id
    ON manacommunity.sports_match_innings(match_result_id);

CREATE INDEX IF NOT EXISTS idx_match_innings_batting_team
    ON manacommunity.sports_match_innings(batting_team_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_match_innings_result_number'
    ) THEN
        ALTER TABLE manacommunity.sports_match_innings
            ADD CONSTRAINT uq_match_innings_result_number
            UNIQUE (match_result_id, innings_number);
    END IF;
END $$;


-- ══ E. sports_batting_performance ════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_batting_perf_innings_id
    ON manacommunity.sports_batting_performance(innings_id);

CREATE INDEX IF NOT EXISTS idx_batting_perf_player_id
    ON manacommunity.sports_batting_performance(player_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_batting_perf_innings_player'
    ) THEN
        ALTER TABLE manacommunity.sports_batting_performance
            ADD CONSTRAINT uq_batting_perf_innings_player
            UNIQUE (innings_id, player_id);
    END IF;
END $$;


-- ══ F. sports_bowling_performance ════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_bowling_perf_innings_id
    ON manacommunity.sports_bowling_performance(innings_id);

CREATE INDEX IF NOT EXISTS idx_bowling_perf_player_id
    ON manacommunity.sports_bowling_performance(player_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_bowling_perf_innings_player'
    ) THEN
        ALTER TABLE manacommunity.sports_bowling_performance
            ADD CONSTRAINT uq_bowling_perf_innings_player
            UNIQUE (innings_id, player_id);
    END IF;
END $$;


-- ══ G. sports_match_event ════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_match_event_match_id
    ON manacommunity.sports_match_event(match_id);

CREATE INDEX IF NOT EXISTS idx_match_event_team_id
    ON manacommunity.sports_match_event(team_id);

CREATE INDEX IF NOT EXISTS idx_match_event_player_id
    ON manacommunity.sports_match_event(player_id);


-- ══ H. sports_match_period_score ═════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_period_score_match_id
    ON manacommunity.sports_match_period_score(match_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_period_score_match_period'
    ) THEN
        ALTER TABLE manacommunity.sports_match_period_score
            ADD CONSTRAINT uq_period_score_match_period
            UNIQUE (match_id, period_number);
    END IF;
END $$;


-- ══ I. sports_player_match_stats ═════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_player_match_stats_match
    ON manacommunity.sports_player_match_stats(match_id);

CREATE INDEX IF NOT EXISTS idx_player_match_stats_player
    ON manacommunity.sports_player_match_stats(player_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_player_match_stats_match_player'
    ) THEN
        ALTER TABLE manacommunity.sports_player_match_stats
            ADD CONSTRAINT uq_player_match_stats_match_player
            UNIQUE (match_id, player_id);
    END IF;
END $$;


-- ══ J. sports_race_result ════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_race_result_match_id
    ON manacommunity.sports_race_result(match_id);

CREATE INDEX IF NOT EXISTS idx_race_result_player_id
    ON manacommunity.sports_race_result(player_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_race_result_match_player'
    ) THEN
        ALTER TABLE manacommunity.sports_race_result
            ADD CONSTRAINT uq_race_result_match_player
            UNIQUE (match_id, player_id);
    END IF;
END $$;


-- ══ K. sports_group_team_standing ════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_group_standing_group_id
    ON manacommunity.sports_group_team_standing(group_id);

CREATE INDEX IF NOT EXISTS idx_group_standing_team_id
    ON manacommunity.sports_group_team_standing(team_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_group_standing_group_team'
    ) THEN
        ALTER TABLE manacommunity.sports_group_team_standing
            ADD CONSTRAINT uq_group_standing_group_team
            UNIQUE (group_id, team_id);
    END IF;
END $$;


-- ══ L. sports_tournament_group ═══════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_tournament_group_config_id
    ON manacommunity.sports_tournament_group(config_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_tournament_group_config_name'
    ) THEN
        ALTER TABLE manacommunity.sports_tournament_group
            ADD CONSTRAINT uq_tournament_group_config_name
            UNIQUE (config_id, group_name);
    END IF;
END $$;


-- ══ M. sports_scoring_config ═════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_scoring_config_config_id
    ON manacommunity.sports_scoring_config(config_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_scoring_config_config_sport'
    ) THEN
        ALTER TABLE manacommunity.sports_scoring_config
            ADD CONSTRAINT uq_scoring_config_config_sport
            UNIQUE (config_id, sport_type);
    END IF;
END $$;


-- ══ N. sports_tournament_config ══════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_tournament_config_event_id
    ON manacommunity.sports_tournament_config(event_id);

CREATE INDEX IF NOT EXISTS idx_tournament_config_community_id
    ON manacommunity.sports_tournament_config(community_id);

CREATE INDEX IF NOT EXISTS idx_tournament_config_sport_id
    ON manacommunity.sports_tournament_config(sport_id);
