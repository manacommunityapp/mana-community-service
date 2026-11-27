-- V405: Parking session entry/exit tracking and advance visitor slot bookings
-- Extends the existing parking_slot (V144) and parking_visitor_pass (V138) tables.

CREATE TABLE IF NOT EXISTS parking_session (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT NOT NULL,
    slot_id          BIGINT REFERENCES parking_slot(id),
    vehicle_number   VARCHAR(30) NOT NULL,
    entry_time       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exit_time        TIMESTAMPTZ,
    session_type     VARCHAR(30) NOT NULL DEFAULT 'VISITOR',
                     -- RESIDENT, VISITOR, DELIVERY, STAFF
    authorized_by    BIGINT,              -- resident user_id who pre-approved
    overstay_flag    BOOLEAN NOT NULL DEFAULT FALSE,
    overstay_notified_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parking_session_community ON parking_session(community_id, entry_time DESC);
CREATE INDEX idx_parking_session_active     ON parking_session(community_id) WHERE exit_time IS NULL;
CREATE INDEX idx_parking_session_slot       ON parking_session(slot_id, entry_time DESC);

CREATE TABLE IF NOT EXISTS parking_slot_booking (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT NOT NULL,
    slot_id          BIGINT REFERENCES parking_slot(id),
    booked_by        BIGINT NOT NULL,     -- resident user_id
    guest_name       VARCHAR(200),
    guest_vehicle    VARCHAR(30),
    booking_date     DATE NOT NULL,
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
                     -- CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parking_slot_booking_date ON parking_slot_booking(community_id, booking_date, status);
CREATE INDEX idx_parking_slot_booking_user ON parking_slot_booking(booked_by, booking_date DESC);
