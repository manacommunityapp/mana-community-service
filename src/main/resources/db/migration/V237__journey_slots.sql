CREATE TABLE IF NOT EXISTS mobility_journey_slot (
    id                BIGSERIAL PRIMARY KEY,
    community_id      BIGINT      NOT NULL REFERENCES community(id),
    created_by        BIGINT      NOT NULL REFERENCES app_user(id),
    journey_type      VARCHAR(30) NOT NULL,
    direction         VARCHAR(20) NOT NULL,
    hub_code          VARCHAR(30) NOT NULL,
    origin            TEXT        NOT NULL,
    destination       TEXT        NOT NULL,
    departure_time    TIME        NOT NULL,
    days_of_week      VARCHAR(40) NOT NULL,
    seat_capacity     SMALLINT    NOT NULL DEFAULT 4,
    luggage_capacity  SMALLINT    NOT NULL DEFAULT 0,
    price_per_seat    NUMERIC(7,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mobility_journey_slot_lookup
    ON mobility_journey_slot (community_id, journey_type, hub_code, status);
