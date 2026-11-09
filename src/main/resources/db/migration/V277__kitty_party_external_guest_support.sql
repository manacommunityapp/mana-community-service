-- ============================================================================
-- V277: Mana Kitty Party & Social Circles: Outside-of-Community Guest Support
-- Schema: manacommunity
-- ============================================================================

-- 1. Circle external membership configuration
ALTER TABLE manacommunity.kitty_circle
    ADD COLUMN IF NOT EXISTS allow_external_members BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS max_external_members INTEGER NOT NULL DEFAULT 3;

-- 2. Circle member external flags and sponsor tracking
ALTER TABLE manacommunity.kitty_circle_member
    ADD COLUMN IF NOT EXISTS is_external BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sponsor_user_id BIGINT REFERENCES manacommunity.app_user(id),
    ADD COLUMN IF NOT EXISTS external_society_name VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_kitty_member_external ON manacommunity.kitty_circle_member(circle_id, is_external);
CREATE INDEX IF NOT EXISTS idx_kitty_member_sponsor ON manacommunity.kitty_circle_member(sponsor_user_id);

-- 3. Housie live game room code for open guest joins
ALTER TABLE manacommunity.kitty_game_room
    ADD COLUMN IF NOT EXISTS room_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS allow_guest_players BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_kitty_game_room_code ON manacommunity.kitty_game_room(room_code) WHERE room_code IS NOT NULL;

-- 4. Event attendance gate pass tracking
ALTER TABLE manacommunity.kitty_event_rsvp
    ADD COLUMN IF NOT EXISTS gate_pass_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS gate_pass_generated_at TIMESTAMP;
