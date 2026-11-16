-- V257: Home services marketplace tables + amenity booking payment columns

-- Home service categories
CREATE TABLE IF NOT EXISTS home_service_category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL,
    icon_url    VARCHAR(300),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Home service providers
CREATE TABLE IF NOT EXISTS home_service_provider (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT       NOT NULL,
    user_id          BIGINT,
    name             VARCHAR(100) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    category_id      BIGINT       NOT NULL REFERENCES home_service_category(id),
    experience_years INT          NOT NULL DEFAULT 0,
    rating           NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count     INT          NOT NULL DEFAULT 0,
    is_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_available     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_home_svc_provider_community ON home_service_provider(community_id);
CREATE INDEX IF NOT EXISTS idx_home_svc_provider_category  ON home_service_provider(category_id);

-- Home service bookings
CREATE TABLE IF NOT EXISTS home_service_booking (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT        NOT NULL,
    resident_user_id BIGINT        NOT NULL,
    provider_id      BIGINT        NOT NULL REFERENCES home_service_provider(id),
    flat_no          VARCHAR(20),
    scheduled_date   DATE          NOT NULL,
    scheduled_time   TIME          NOT NULL,
    description      TEXT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    amount           NUMERIC(12,2),
    payment_status   VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    resident_rating  INT,
    resident_review  TEXT,
    cancelled_reason TEXT,
    completed_at     TIMESTAMP,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_home_svc_booking_community ON home_service_booking(community_id);
CREATE INDEX IF NOT EXISTS idx_home_svc_booking_resident  ON home_service_booking(community_id, resident_user_id);
CREATE INDEX IF NOT EXISTS idx_home_svc_booking_status    ON home_service_booking(community_id, status);

-- Amenity booking: payment columns
ALTER TABLE resource_booking
    ADD COLUMN IF NOT EXISTS invoice_id   BIGINT       NULL,
    ADD COLUMN IF NOT EXISTS payment_note VARCHAR(500) NULL;
