-- Insta Help on-demand home services booking table
CREATE TABLE insta_help_booking (
    id                   BIGSERIAL PRIMARY KEY,
    booking_ref          VARCHAR(20)    NOT NULL UNIQUE,
    community_id         BIGINT         NOT NULL REFERENCES community(id),
    resident_id          BIGINT         NOT NULL REFERENCES app_user(id),
    service_type         VARCHAR(20)    NOT NULL,
    subtask              VARCHAR(100),
    duration             VARCHAR(15)    NOT NULL,
    price_amount         NUMERIC(10, 2),
    notes                VARCHAR(500),
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    closure_otp          VARCHAR(6),
    helper_name          VARCHAR(100),
    helper_rating        DOUBLE PRECISION,
    helper_eta_minutes   INTEGER,
    helper_distance_label VARCHAR(100),
    resident_rating      INTEGER,
    resident_feedback    VARCHAR(500),
    booked_at            TIMESTAMP      NOT NULL,
    matched_at           TIMESTAMP,
    started_at           TIMESTAMP,
    completed_at         TIMESTAMP,
    cancelled_at         TIMESTAMP
);

CREATE INDEX idx_ihb_community   ON insta_help_booking (community_id);
CREATE INDEX idx_ihb_resident    ON insta_help_booking (resident_id);
CREATE INDEX idx_ihb_status      ON insta_help_booking (status);
CREATE INDEX idx_ihb_booking_ref ON insta_help_booking (booking_ref);
