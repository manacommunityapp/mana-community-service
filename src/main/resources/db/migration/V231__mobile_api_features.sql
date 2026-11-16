-- V100: Mobile API features - vehicle gate whitelist, broadcast columns, KYC, school bus

-- Add gate_whitelisted to resident_vehicle
ALTER TABLE resident_vehicle
    ADD COLUMN IF NOT EXISTS gate_whitelisted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add category/target columns to announcement for broadcast API
ALTER TABLE announcement
    ADD COLUMN IF NOT EXISTS broadcast_category VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS broadcast_target    VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS reach_count         INT         NOT NULL DEFAULT 0;

-- KYC documents for residents
CREATE TABLE IF NOT EXISTS resident_kyc_document (
    id              BIGSERIAL     PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    community_id    BIGINT        NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    document_type   VARCHAR(30)   NOT NULL,
    document_number VARCHAR(100)  NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING_REVIEW',
    front_image_url TEXT          NULL,
    back_image_url  TEXT          NULL,
    rejection_reason TEXT         NULL,
    submitted_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMP     NULL,
    reviewed_by     BIGINT        NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kyc_user     ON resident_kyc_document(user_id);
CREATE INDEX IF NOT EXISTS idx_kyc_community ON resident_kyc_document(community_id);

-- School bus tables
CREATE TABLE IF NOT EXISTS school_bus (
    id              BIGSERIAL     PRIMARY KEY,
    community_id    BIGINT        NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    bus_number      VARCHAR(30)   NOT NULL,
    driver_name     VARCHAR(100)  NOT NULL,
    driver_phone    VARCHAR(20)   NULL,
    attendant_name  VARCHAR(100)  NULL,
    attendant_phone VARCHAR(20)   NULL,
    school_name     VARCHAR(150)  NOT NULL,
    route_name      VARCHAR(100)  NOT NULL DEFAULT 'Main Route',
    status          VARCHAR(30)   NOT NULL DEFAULT 'IDLE',
    current_lat     DECIMAL(10,7) NULL,
    current_lng     DECIMAL(10,7) NULL,
    location_updated_at TIMESTAMP NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS school_bus_registered_child (
    id              BIGSERIAL     PRIMARY KEY,
    community_id    BIGINT        NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    user_id         BIGINT        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    bus_id          BIGINT        NULL REFERENCES school_bus(id) ON DELETE SET NULL,
    name            VARCHAR(100)  NOT NULL,
    grade           VARCHAR(50)   NOT NULL,
    school_name     VARCHAR(150)  NOT NULL,
    stop_name       VARCHAR(100)  NULL,
    photo_url       TEXT          NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bus_child_user      ON school_bus_registered_child(user_id);
CREATE INDEX IF NOT EXISTS idx_bus_child_community ON school_bus_registered_child(community_id);

CREATE TABLE IF NOT EXISTS school_bus_trip_log (
    id              BIGSERIAL     PRIMARY KEY,
    bus_id          BIGINT        NOT NULL REFERENCES school_bus(id) ON DELETE CASCADE,
    child_id        BIGINT        NOT NULL REFERENCES school_bus_registered_child(id) ON DELETE CASCADE,
    direction       VARCHAR(20)   NOT NULL,
    trip_date       DATE          NOT NULL,
    departure_time  VARCHAR(10)   NULL,
    arrival_time    VARCHAR(10)   NULL,
    child_picked_up BOOLEAN       NOT NULL DEFAULT FALSE,
    picked_up_at    VARCHAR(10)   NULL,
    dropped_off_at  VARCHAR(10)   NULL,
    notes           TEXT          NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trip_child ON school_bus_trip_log(child_id);
CREATE INDEX IF NOT EXISTS idx_trip_bus   ON school_bus_trip_log(bus_id);
CREATE INDEX IF NOT EXISTS idx_trip_date  ON school_bus_trip_log(trip_date);
