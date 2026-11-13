-- V243: Add feed_post_policy and post_to_feed columns for granular sports feed publishing
-- Allows decoupling "Live Now" strip scoreboard from permanent community feed result posts,
-- and allows filtering which stages (e.g. FINALS_ONLY, KNOCKOUTS_ONLY) publish feed posts.

-- 1. Tournament level feed post policy
ALTER TABLE sports_tournament
ADD COLUMN IF NOT EXISTS feed_post_policy VARCHAR(30) DEFAULT 'FINALS_ONLY',
ADD COLUMN IF NOT EXISTS feed_post_stages VARCHAR(255);

-- 2. Event level feed post policy (null = inherit from tournament)
ALTER TABLE sports_event
ADD COLUMN IF NOT EXISTS feed_post_policy VARCHAR(30),
ADD COLUMN IF NOT EXISTS feed_post_stages VARCHAR(255);

-- 3. Match level feed post override (null = inherit from event/tournament policy, true = force post, false = force skip)
ALTER TABLE sports_tournament_match
ADD COLUMN IF NOT EXISTS post_to_feed BOOLEAN;

COMMENT ON COLUMN sports_tournament.feed_post_policy IS 'Policy for auto-posting completed match results to feed: FINALS_ONLY, KNOCKOUTS_ONLY, ALL_MATCHES, NONE, CUSTOM';
COMMENT ON COLUMN sports_tournament.feed_post_stages IS 'Comma-separated stages to post if policy is CUSTOM (e.g. FINAL,SEMI_FINAL)';
COMMENT ON COLUMN sports_event.feed_post_policy IS 'Event-level policy override for posting results to feed (null = inherit from tournament)';
COMMENT ON COLUMN sports_event.feed_post_stages IS 'Event-level custom stages override (null = inherit from tournament)';
COMMENT ON COLUMN sports_tournament_match.post_to_feed IS 'Match-level override for posting completed result to feed (null = inherit from policy)';
