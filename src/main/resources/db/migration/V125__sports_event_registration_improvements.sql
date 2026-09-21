-- V125: Sports event registration table improvements

-- 1. Add reviewed_at timestamp for admin audit trail
ALTER TABLE manacommunity.sports_event_registration
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

-- Backfill reviewed_at from updated_at for already-reviewed registrations
UPDATE manacommunity.sports_event_registration
SET reviewed_at = updated_at
WHERE reviewed_at IS NULL
  AND reviewed_by_user_id IS NOT NULL
  AND status IN ('CONFIRMED', 'REJECTED');

-- 2. Add compound index for the hot-path existsByEventIdAndUserIdAndStatusIn query
CREATE INDEX IF NOT EXISTS idx_reg_event_user_status
    ON manacommunity.sports_event_registration(event_id, user_id, status);

-- 3. Add partial unique index to prevent concurrent duplicate registrations
-- Covers the Java duplicate check: same event + player_name + email + flat_number
CREATE UNIQUE INDEX IF NOT EXISTS uq_reg_event_player
    ON manacommunity.sports_event_registration(event_id, LOWER(player_name), LOWER(email), LOWER(flat_number))
    WHERE status NOT IN ('WITHDRAWN', 'REJECTED');

-- 4. Add length constraints
ALTER TABLE manacommunity.sports_event_registration
    ALTER COLUMN player_name TYPE VARCHAR(150);
ALTER TABLE manacommunity.sports_event_registration
    ALTER COLUMN role TYPE VARCHAR(30);

-- 5. Default captain fields to false
ALTER TABLE manacommunity.sports_event_registration
    ALTER COLUMN captain_nomination SET DEFAULT FALSE;
ALTER TABLE manacommunity.sports_event_registration
    ALTER COLUMN captain_confirmation SET DEFAULT FALSE;

UPDATE manacommunity.sports_event_registration
SET captain_nomination = FALSE WHERE captain_nomination IS NULL;
UPDATE manacommunity.sports_event_registration
SET captain_confirmation = FALSE WHERE captain_confirmation IS NULL;
