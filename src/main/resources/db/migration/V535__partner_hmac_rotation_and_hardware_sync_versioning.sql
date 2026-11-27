-- Partner HMAC Dual-Key Rotation, Nonce Replay Journal & Deprecated API Telemetry

CREATE TABLE IF NOT EXISTS partner_webhook_keys (
    id BIGSERIAL PRIMARY KEY,
    partner_id VARCHAR(64) NOT NULL,
    key_id VARCHAR(64) NOT NULL UNIQUE,
    secret_hex VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    grace_until TIMESTAMP,
    revoked_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_partner_keys_partner_status
    ON partner_webhook_keys (partner_id, status);

CREATE TABLE IF NOT EXISTS webhook_nonce_journal (
    id BIGSERIAL PRIMARY KEY,
    partner_id VARCHAR(64) NOT NULL,
    nonce VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_webhook_nonce_partner UNIQUE (partner_id, nonce)
);

CREATE TABLE IF NOT EXISTS deprecated_api_telemetry (
    id BIGSERIAL PRIMARY KEY,
    endpoint VARCHAR(128) NOT NULL,
    api_version VARCHAR(16) NOT NULL,
    controller_serial VARCHAR(64) NOT NULL,
    community_id BIGINT,
    firmware_version VARCHAR(64),
    call_count BIGINT NOT NULL DEFAULT 1,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_deprecated_endpoint_serial UNIQUE (endpoint, controller_serial)
);
