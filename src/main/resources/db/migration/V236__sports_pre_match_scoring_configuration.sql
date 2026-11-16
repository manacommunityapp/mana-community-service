-- Flyway migration: V236__sports_pre_match_scoring_configuration.sql
-- Adds pre-match scoring parameters (cricket overs, bowler limits, break duration, target scores, and match-level override)

ALTER TABLE manacommunity.sports_scoring_config
    ADD COLUMN IF NOT EXISTS overs_per_innings INT,
    ADD COLUMN IF NOT EXISTS max_overs_per_bowler INT,
    ADD COLUMN IF NOT EXISTS balls_per_over INT DEFAULT 6,
    ADD COLUMN IF NOT EXISTS break_duration_minutes INT,
    ADD COLUMN IF NOT EXISTS target_score INT;

ALTER TABLE manacommunity.sports_tournament_match
    ADD COLUMN IF NOT EXISTS match_config_override_json TEXT;
