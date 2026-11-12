ALTER TABLE mobility_carpool_offer
    ADD COLUMN IF NOT EXISTS journey_type VARCHAR(30) NOT NULL DEFAULT 'GENERIC',
    ADD COLUMN IF NOT EXISTS hub_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS context_name VARCHAR(160);

CREATE INDEX IF NOT EXISTS idx_mobility_journey_offers
    ON mobility_carpool_offer (community_id, journey_type, hub_code, departure_time);

ALTER TABLE mobility_airport_ride_request
    ALTER COLUMN airport_code DROP NOT NULL;

ALTER TABLE mobility_airport_ride_request
    ADD COLUMN IF NOT EXISTS journey_type VARCHAR(30) NOT NULL DEFAULT 'AIRPORT',
    ADD COLUMN IF NOT EXISTS hub_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS context_name VARCHAR(160);
