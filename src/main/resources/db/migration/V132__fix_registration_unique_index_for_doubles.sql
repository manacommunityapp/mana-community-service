-- V132: Fix registration unique index to allow same player in different match formats
-- The existing uq_reg_event_player index blocks a player from registering for both
-- SINGLES and DOUBLES in the same event because it doesn't include match_type.

DROP INDEX IF EXISTS manacommunity.uq_reg_event_player;

CREATE UNIQUE INDEX uq_reg_event_player
    ON manacommunity.sports_event_registration(event_id, LOWER(player_name), LOWER(email), LOWER(flat_number), COALESCE(match_type, 'SINGLES'))
    WHERE status NOT IN ('WITHDRAWN', 'REJECTED');
