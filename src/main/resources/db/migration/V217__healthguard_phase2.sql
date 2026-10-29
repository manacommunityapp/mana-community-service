-- HealthGuard Phase 2: Community Responder Network + Ambulance Booking

CREATE TABLE IF NOT EXISTS manacommunity.community_responder (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    skills              TEXT,
    certifications      TEXT,
    phone_number        VARCHAR(20),
    total_response_count INT NOT NULL DEFAULT 0,
    last_ping_at        TIMESTAMP,
    registered_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_community_responder_user_id ON manacommunity.community_responder(user_id);
CREATE INDEX IF NOT EXISTS idx_community_responder_active_status ON manacommunity.community_responder(is_active, availability_status);

CREATE TABLE IF NOT EXISTS manacommunity.ambulance_booking (
    id                          BIGSERIAL PRIMARY KEY,
    booking_uuid                VARCHAR(36) NOT NULL UNIQUE,
    user_id                     BIGINT NOT NULL,
    incident_id                 BIGINT,
    status                      VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    pickup_address              TEXT,
    destination_address         TEXT,
    patient_name                VARCHAR(200),
    patient_phone               VARCHAR(20),
    emergency_description       TEXT,
    vehicle_number              VARCHAR(50),
    driver_name                 VARCHAR(200),
    driver_phone                VARCHAR(20),
    estimated_arrival_minutes   INT,
    requested_at                TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at                TIMESTAMP,
    dispatched_at               TIMESTAMP,
    arrived_at                  TIMESTAMP,
    completed_at                TIMESTAMP,
    notes                       TEXT
);

CREATE INDEX IF NOT EXISTS idx_ambulance_booking_user_id ON manacommunity.ambulance_booking(user_id);
CREATE INDEX IF NOT EXISTS idx_ambulance_booking_status ON manacommunity.ambulance_booking(status);
CREATE INDEX IF NOT EXISTS idx_ambulance_booking_user_status ON manacommunity.ambulance_booking(user_id, status);
