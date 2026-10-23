-- Replaces the JSON blob in event_pooja_user_registrations.attending_devotees with proper rows.
-- attending_devotees is kept for backwards compatibility; participants table is authoritative.

CREATE TABLE IF NOT EXISTS event_pooja_booking_participants (
    id              BIGSERIAL    PRIMARY KEY,
    registration_id BIGINT       NOT NULL,
    name            VARCHAR(255) NOT NULL,
    gotram          VARCHAR(100),
    nakshatra       VARCHAR(100),
    relation        VARCHAR(50),        -- head / spouse / child / parent / other
    qr_code_url     TEXT,
    checked_in      BOOLEAN      NOT NULL DEFAULT FALSE,
    checked_in_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_epbp_registration
        FOREIGN KEY (registration_id)
        REFERENCES event_pooja_user_registrations(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_epbp_registration_id
    ON event_pooja_booking_participants (registration_id);
