ALTER TABLE mobility_carpool_offer
    ADD COLUMN IF NOT EXISTS journey_direction VARCHAR(20);

UPDATE mobility_carpool_offer
SET journey_direction = airport_direction
WHERE journey_direction IS NULL AND airport_direction IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mobility_journey_direction
    ON mobility_carpool_offer (community_id, journey_type, hub_code, journey_direction, departure_time);
