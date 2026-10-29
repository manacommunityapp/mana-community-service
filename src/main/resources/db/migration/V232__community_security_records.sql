CREATE TABLE IF NOT EXISTS community_security_record (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES community(id),
    actor_id BIGINT REFERENCES app_user(id),
    resident_id BIGINT REFERENCES app_user(id),
    record_type VARCHAR(40) NOT NULL,
    name VARCHAR(150), phone VARCHAR(30), vehicle_number VARCHAR(30),
    valid_from VARCHAR(40), valid_until VARCHAR(40), time_window VARCHAR(100),
    purpose VARCHAR(500), location VARCHAR(200), status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT, otp_hash VARCHAR(128), otp_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_security_record_scope ON community_security_record(community_id, record_type, status);
CREATE INDEX IF NOT EXISTS idx_security_record_resident ON community_security_record(resident_id, record_type);
