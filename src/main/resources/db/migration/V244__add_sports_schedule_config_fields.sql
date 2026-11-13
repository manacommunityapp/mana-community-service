-- V244: Add advanced tournament scheduling fields to sports_tournament_config
-- Supports:
-- 1. seeding_order (RANDOM, TRADITIONAL, SEQUENTIAL, RANKING)
-- 2. byes_first_round_only (Give all BYEs in 1st knockout round only)
-- 3. show_byes_in_bracket (Show BYE match slots in bracket draw)
-- 4. limit_matches_per_day & max_matches_per_day (Limit participant matches per day)
-- 5. use_playoff_days & playoff_days (Custom knockout / playoff days)
-- 6. venue_assign_type (Random, Snake, Sequential, Parallel)
-- 7. playoff_round_timings_json (Per-round playoff dates, times, durations & modes)

ALTER TABLE sports_tournament_config
ADD COLUMN IF NOT EXISTS seeding_order VARCHAR(30) DEFAULT 'RANDOM',
ADD COLUMN IF NOT EXISTS byes_first_round_only BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS show_byes_in_bracket BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS limit_matches_per_day BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS max_matches_per_day INT DEFAULT 2,
ADD COLUMN IF NOT EXISTS use_playoff_days BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS playoff_days VARCHAR(50),
ADD COLUMN IF NOT EXISTS venue_assign_type VARCHAR(30) DEFAULT 'Random',
ADD COLUMN IF NOT EXISTS playoff_round_timings_json TEXT;

COMMENT ON COLUMN sports_tournament_config.seeding_order IS 'Seeding draw order: RANDOM, TRADITIONAL, SEQUENTIAL, RANKING';
COMMENT ON COLUMN sports_tournament_config.byes_first_round_only IS 'Give all BYEs in 1st knockout round only';
COMMENT ON COLUMN sports_tournament_config.show_byes_in_bracket IS 'Show BYE match slots in bracket draw (16/32/64/128)';
COMMENT ON COLUMN sports_tournament_config.limit_matches_per_day IS 'Enforce maximum matches per participant per day';
COMMENT ON COLUMN sports_tournament_config.max_matches_per_day IS 'Maximum matches per participant per day limit';
COMMENT ON COLUMN sports_tournament_config.use_playoff_days IS 'Use separate custom days for playoffs / knockout rounds';
COMMENT ON COLUMN sports_tournament_config.playoff_days IS 'Comma-separated day-of-week indexes for playoffs (e.g. 0,6 for weekends)';
COMMENT ON COLUMN sports_tournament_config.venue_assign_type IS 'Court assignment algorithm: Random, Snake, Sequential, Parallel';
COMMENT ON COLUMN sports_tournament_config.playoff_round_timings_json IS 'JSON-encoded per-round playoff scheduling details: dates, times, durations, and modes';
