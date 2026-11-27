-- Compact Low-Signal Lift SOS Ingestion & Sub-2s Geo-Targeted Tower Evacuation Roster

ALTER TABLE sos_incident
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ingest_channel VARCHAR(24) NOT NULL DEFAULT 'HTTP',
    ADD COLUMN IF NOT EXISTS siren_override BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS nearest_guard_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_sos_idempotency_key
    ON sos_incident (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS evacuation_broadcasts (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    tower_block VARCHAR(50) NOT NULL,
    emergency_type VARCHAR(32) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'CRITICAL',
    message TEXT NOT NULL,
    assembly_point VARCHAR(200),
    residents_count INT NOT NULL DEFAULT 0,
    staff_count INT NOT NULL DEFAULT 0,
    visitors_count INT NOT NULL DEFAULT 0,
    sms_fallback_count INT NOT NULL DEFAULT 0,
    triggered_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evacuation_muster_roster (
    id BIGSERIAL PRIMARY KEY,
    broadcast_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    tower_block VARCHAR(50) NOT NULL,
    person_category VARCHAR(24) NOT NULL,
    person_id BIGINT,
    person_name VARCHAR(120) NOT NULL,
    contact_phone VARCHAR(32),
    flat_number VARCHAR(32),
    status VARCHAR(24) NOT NULL DEFAULT 'ALERTED',
    marked_safe_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evac_roster_broadcast_status
    ON evacuation_muster_roster (broadcast_id, status);
