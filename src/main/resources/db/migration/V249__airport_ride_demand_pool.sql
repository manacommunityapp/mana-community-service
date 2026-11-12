CREATE TABLE mobility_airport_ride_request (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT      NOT NULL REFERENCES community(id),
    requester_id        BIGINT      NOT NULL REFERENCES app_user(id),
    direction           VARCHAR(20) NOT NULL,
    airport_code        VARCHAR(3)  NOT NULL,
    area                TEXT        NOT NULL,
    travel_at           TIMESTAMPTZ NOT NULL,
    time_window_minutes SMALLINT    NOT NULL DEFAULT 30,
    seats               SMALLINT    NOT NULL DEFAULT 1,
    luggage_count       SMALLINT    NOT NULL DEFAULT 0,
    flight_number       VARCHAR(20),
    terminal            VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_airport_request_match
    ON mobility_airport_ride_request (community_id, airport_code, direction, travel_at, status);
CREATE INDEX idx_airport_request_user
    ON mobility_airport_ride_request (requester_id, created_at DESC);
