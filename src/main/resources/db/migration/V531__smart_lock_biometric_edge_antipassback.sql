-- Smart Locks (HKDF Offline PINs), Biometric Edge Delta Sync & Turnstile Anti-Passback

ALTER TABLE smart_devices
    ADD COLUMN IF NOT EXISTS master_secret_hex VARCHAR(128),
    ADD COLUMN IF NOT EXISTS rtc_drift_seconds INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_rtc_sync_at TIMESTAMP;

ALTER TABLE device_visitor_otps
    ADD COLUMN IF NOT EXISTS derivation_mode VARCHAR(24) NOT NULL DEFAULT 'ONLINE_RANDOM',
    ADD COLUMN IF NOT EXISTS slot_epoch_start BIGINT,
    ADD COLUMN IF NOT EXISTS slot_duration_buckets INT,
    ADD COLUMN IF NOT EXISTS single_use BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE biometric_user_enrollments
    ADD COLUMN IF NOT EXISTS sync_version BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS embedding_vector_b64 TEXT,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS biometric_edge_deltas (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    sync_version BIGINT NOT NULL,
    op VARCHAR(16) NOT NULL,
    enrollment_id BIGINT,
    person_id BIGINT NOT NULL,
    person_name VARCHAR(120),
    face_embedding_hash VARCHAR(255) NOT NULL,
    embedding_vector_b64 TEXT,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_biometric_delta_comm_ver
    ON biometric_edge_deltas (community_id, sync_version);

CREATE TABLE IF NOT EXISTS turnstile_apb_states (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    zone_id VARCHAR(64) NOT NULL,
    credential_id VARCHAR(128) NOT NULL,
    current_state VARCHAR(16) NOT NULL DEFAULT 'OUTSIDE',
    last_direction VARCHAR(16) NOT NULL DEFAULT 'EXIT',
    last_turnstile_id VARCHAR(64),
    entered_at TIMESTAMP,
    exited_at TIMESTAMP,
    last_event_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    soft_override_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_turnstile_apb_cred UNIQUE (community_id, zone_id, credential_id)
);
