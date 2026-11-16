ALTER TABLE mobility_carpool_offer
    ADD COLUMN IF NOT EXISTS airport_direction VARCHAR(20),
    ADD COLUMN IF NOT EXISTS airport_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS airport_name VARCHAR(160),
    ADD COLUMN IF NOT EXISTS flight_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS terminal VARCHAR(20),
    ADD COLUMN IF NOT EXISTS luggage_capacity SMALLINT;

CREATE INDEX IF NOT EXISTS idx_mobility_airport_rides
    ON mobility_carpool_offer (community_id, airport_code, airport_direction, departure_time)
    WHERE airport_code IS NOT NULL;
