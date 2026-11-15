-- V241: Add show_in_feed and feed_result_post_id columns for community feed sports integration

-- 1. Tournament level visibility
ALTER TABLE sports_tournament
ADD COLUMN IF NOT EXISTS show_in_feed BOOLEAN;

-- 2. Event level visibility (null = inherit from tournament)
ALTER TABLE sports_event
ADD COLUMN IF NOT EXISTS show_in_feed BOOLEAN;

-- 3. Match level visibility (null = inherit from event/tournament)
ALTER TABLE sports_tournament_match
ADD COLUMN IF NOT EXISTS show_in_feed BOOLEAN;

-- 4. Track feed result post ID to prevent duplicate sports result posts on match completion
ALTER TABLE sports_tournament_match
ADD COLUMN IF NOT EXISTS feed_result_post_id BIGINT;

COMMENT ON COLUMN sports_tournament.show_in_feed IS 'Whether to show live matches of this tournament on the community feed';
COMMENT ON COLUMN sports_event.show_in_feed IS 'Event-level override for showing live matches on community feed (null = inherit from tournament)';
COMMENT ON COLUMN sports_tournament_match.show_in_feed IS 'Match-level override for showing live match on community feed (null = inherit from event/tournament)';
COMMENT ON COLUMN sports_tournament_match.feed_result_post_id IS 'ID of the published SPORTS result post on the community feed';
