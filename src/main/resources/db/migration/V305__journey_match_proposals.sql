CREATE TABLE IF NOT EXISTS mobility_journey_match (
    id           BIGSERIAL PRIMARY KEY,
    request_id   BIGINT      NOT NULL REFERENCES mobility_airport_ride_request(id),
    offer_id     BIGINT      NOT NULL REFERENCES mobility_carpool_offer(id),
    requester_id BIGINT      NOT NULL REFERENCES app_user(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,
    CONSTRAINT uk_mobility_journey_match_request_offer UNIQUE (request_id, offer_id)
);

CREATE INDEX IF NOT EXISTS idx_mobility_journey_match_requester
    ON mobility_journey_match (requester_id, created_at DESC);
