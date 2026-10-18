-- ─────────────────────────────────────────────────────────────────
-- V200 · Mana Mobility Hub
--   Tables: mobility_driver, mobility_vehicle, mobility_driver_document,
--           mobility_service_zone, mobility_trip, mobility_driver_offer,
--           mobility_carpool_offer, mobility_carpool_booking,
--           mobility_driver_earnings_daily
-- ─────────────────────────────────────────────────────────────────

-- Driver profile (one per app_user)
CREATE TABLE mobility_driver (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL UNIQUE REFERENCES app_user(id),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    availability        VARCHAR(10)  NOT NULL DEFAULT 'OFFLINE',
    current_lat         DOUBLE PRECISION,
    current_lng         DOUBLE PRECISION,
    location_updated_at TIMESTAMPTZ,
    avg_rating          NUMERIC(3,2) NOT NULL DEFAULT 0,
    total_trips         INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Vehicles registered by a driver
CREATE TABLE mobility_vehicle (
    id            BIGSERIAL PRIMARY KEY,
    driver_id     BIGINT      NOT NULL REFERENCES mobility_driver(id),
    vehicle_type  VARCHAR(20) NOT NULL,  -- BIKE, AUTO, CAB, VAN, BUS
    make          VARCHAR(60),
    model         VARCHAR(60) NOT NULL,
    number_plate  VARCHAR(20) NOT NULL,
    color         VARCHAR(40),
    year          INT,
    is_active     BOOLEAN     NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- KYC documents uploaded by a driver
CREATE TABLE mobility_driver_document (
    id            BIGSERIAL PRIMARY KEY,
    driver_id     BIGINT      NOT NULL REFERENCES mobility_driver(id),
    doc_type      VARCHAR(30) NOT NULL,  -- DRIVING_LICENCE, VEHICLE_RC, INSURANCE, PAN_CARD, AADHAAR
    file_url      TEXT        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    reject_reason TEXT,
    reviewed_by   BIGINT      REFERENCES app_user(id),
    reviewed_at   TIMESTAMPTZ,
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Service zones (geo-fenced areas where mobility is active)
CREATE TABLE mobility_service_zone (
    id             BIGSERIAL PRIMARY KEY,
    community_id   BIGINT      NOT NULL REFERENCES community(id),
    name           VARCHAR(100) NOT NULL,
    center_lat     DOUBLE PRECISION NOT NULL,
    center_lng     DOUBLE PRECISION NOT NULL,
    radius_km      NUMERIC(5,2) NOT NULL DEFAULT 5.0,
    is_active      BOOLEAN     NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Core trip table
CREATE TABLE mobility_trip (
    id               BIGSERIAL PRIMARY KEY,
    trip_ref         VARCHAR(16)  NOT NULL UNIQUE,  -- e.g. MB-20241801-0001
    community_id     BIGINT       NOT NULL REFERENCES community(id),
    passenger_id     BIGINT       NOT NULL REFERENCES app_user(id),
    driver_id        BIGINT       REFERENCES mobility_driver(id),
    vehicle_id       BIGINT       REFERENCES mobility_vehicle(id),

    service_type     VARCHAR(20)  NOT NULL,  -- CARPOOL, GROUP_RIDE, BIKE, AUTO, CAB
    status           VARCHAR(25)  NOT NULL DEFAULT 'REQUESTED',

    pickup_lat       DOUBLE PRECISION NOT NULL,
    pickup_lng       DOUBLE PRECISION NOT NULL,
    pickup_address   TEXT         NOT NULL,
    dest_lat         DOUBLE PRECISION NOT NULL,
    dest_lng         DOUBLE PRECISION NOT NULL,
    dest_address     TEXT         NOT NULL,

    pickup_pin       VARCHAR(6),
    idempotency_key  VARCHAR(64)  NOT NULL,

    -- Fare snapshot from quote
    quote_id         VARCHAR(64),
    base_fare        NUMERIC(10,2) NOT NULL DEFAULT 0,
    distance_fare    NUMERIC(10,2) NOT NULL DEFAULT 0,
    duration_fare    NUMERIC(10,2) NOT NULL DEFAULT 0,
    taxes            NUMERIC(10,2) NOT NULL DEFAULT 0,
    quoted_total     NUMERIC(10,2) NOT NULL DEFAULT 0,
    final_fare       NUMERIC(10,2),

    distance_km      NUMERIC(7,2),
    eta_minutes      INT,
    payment_method   VARCHAR(20)  NOT NULL DEFAULT 'CASH',
    promo_code       VARCHAR(30),
    cancel_reason    TEXT,
    passenger_rating SMALLINT,
    driver_rating    SMALLINT,

    requested_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    assigned_at      TIMESTAMPTZ,
    arrived_at       TIMESTAMPTZ,
    picked_up_at     TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    cancelled_at     TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_mobility_trip_idempotency ON mobility_trip(passenger_id, idempotency_key);
CREATE INDEX idx_mobility_trip_passenger   ON mobility_trip(passenger_id, requested_at DESC);
CREATE INDEX idx_mobility_trip_driver      ON mobility_trip(driver_id, requested_at DESC);
CREATE INDEX idx_mobility_trip_status      ON mobility_trip(status);
CREATE INDEX idx_mobility_trip_community   ON mobility_trip(community_id, requested_at DESC);

-- Pending driver offer (point-in-time dispatch row; deleted once accepted/declined/expired)
CREATE TABLE mobility_driver_offer (
    id           BIGSERIAL PRIMARY KEY,
    trip_id      BIGINT      NOT NULL REFERENCES mobility_trip(id),
    driver_id    BIGINT      NOT NULL REFERENCES mobility_driver(id),
    offered_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    accepted     BOOLEAN     -- null=pending, true=accepted, false=declined
);
CREATE INDEX idx_mobility_offer_driver ON mobility_driver_offer(driver_id, offered_at DESC);

-- Community carpool offers (separate from on-demand trips)
CREATE TABLE mobility_carpool_offer (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL REFERENCES community(id),
    driver_user_id  BIGINT       NOT NULL REFERENCES app_user(id),
    origin          TEXT         NOT NULL,
    destination     TEXT         NOT NULL,
    departure_time  TIMESTAMPTZ  NOT NULL,
    total_seats     SMALLINT     NOT NULL DEFAULT 3,
    available_seats SMALLINT     NOT NULL DEFAULT 3,
    price_per_seat  NUMERIC(7,2) NOT NULL DEFAULT 0,
    vehicle_model   VARCHAR(80),
    vehicle_number  VARCHAR(20),
    driver_rating   NUMERIC(3,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, FULL, COMPLETED, CANCELLED
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mobility_carpool_community ON mobility_carpool_offer(community_id, departure_time);

CREATE TABLE mobility_carpool_booking (
    id          BIGSERIAL PRIMARY KEY,
    offer_id    BIGINT      NOT NULL REFERENCES mobility_carpool_offer(id),
    user_id     BIGINT      NOT NULL REFERENCES app_user(id),
    seats       SMALLINT    NOT NULL DEFAULT 1,
    status      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    booked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (offer_id, user_id)
);

-- Daily earnings ledger per driver
CREATE TABLE mobility_driver_earnings_daily (
    id          BIGSERIAL PRIMARY KEY,
    driver_id   BIGINT       NOT NULL REFERENCES mobility_driver(id),
    date        DATE         NOT NULL,
    trips       INT          NOT NULL DEFAULT 0,
    gross       NUMERIC(10,2) NOT NULL DEFAULT 0,
    platform_cut NUMERIC(10,2) NOT NULL DEFAULT 0,
    net         NUMERIC(10,2) NOT NULL DEFAULT 0,
    settled     BOOLEAN      NOT NULL DEFAULT false,
    UNIQUE (driver_id, date)
);
