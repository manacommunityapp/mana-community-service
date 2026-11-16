ALTER TABLE mobility_airport_ride_request
    ADD COLUMN IF NOT EXISTS last_match_notified_at TIMESTAMPTZ;
