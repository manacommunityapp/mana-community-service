-- Multi-Phase Township Federation Routing & Privacy-Preserving City-Wide Risk Network

CREATE TABLE IF NOT EXISTS township_federations (
    id BIGSERIAL PRIMARY KEY,
    federation_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    city_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS federation_sub_societies (
    id BIGSERIAL PRIMARY KEY,
    federation_id BIGINT NOT NULL,
    society_id BIGINT NOT NULL UNIQUE,
    phase_label VARCHAR(64) NOT NULL,
    outer_gate_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS federation_transit_tokens (
    id BIGSERIAL PRIMARY KEY,
    transit_id VARCHAR(64) NOT NULL UNIQUE,
    federation_id BIGINT NOT NULL,
    target_society_id BIGINT NOT NULL,
    opaque_flat_label VARCHAR(128) NOT NULL, -- e.g. "Phase 2 - Tower B-404" (no ledger/owner PII)
    identifier_hash VARCHAR(64) NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_federation_transit_target
    ON federation_transit_tokens (target_society_id, valid_until)
    WHERE consumed_at IS NULL;

CREATE TABLE IF NOT EXISTS city_risk_signals (
    id BIGSERIAL PRIMARY KEY,
    city_code VARCHAR(32) NOT NULL,
    subject_token_hash VARCHAR(64) NOT NULL,
    society_blind_hash VARCHAR(64) NOT NULL,
    category VARCHAR(40) NOT NULL, -- DELIVERY_IMPERSONATION, THEFT_FIR, ASSAULT_THREAT, LOCAL_DISPUTE
    visibility VARCHAR(24) NOT NULL, -- CITY_ELIGIBLE, LOCAL_ONLY
    verified_by_mc BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_on_appeal BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_city_risk_blind_corroboration UNIQUE (city_code, subject_token_hash, society_blind_hash)
);

CREATE INDEX IF NOT EXISTS idx_city_risk_lookup
    ON city_risk_signals (city_code, subject_token_hash, visibility, revoked_on_appeal, expires_at);
