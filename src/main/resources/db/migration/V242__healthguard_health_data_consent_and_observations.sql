CREATE TABLE manacommunity.health_data_consent (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    data_scope VARCHAR(32) NOT NULL,
    consent_version VARCHAR(40) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_health_data_consent UNIQUE (user_id, provider, data_scope, consent_version)
);

CREATE INDEX idx_health_data_consent_user_active
    ON manacommunity.health_data_consent (user_id, provider, revoked_at);

CREATE TABLE manacommunity.health_observation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    source_record_id VARCHAR(160) NOT NULL,
    source_app VARCHAR(160),
    observation_type VARCHAR(32) NOT NULL,
    numeric_value NUMERIC(20, 6),
    unit VARCHAR(32),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_health_observation_source UNIQUE (user_id, provider, source_record_id),
    CONSTRAINT ck_health_observation_range CHECK (ended_at >= started_at)
);

CREATE INDEX idx_health_observation_user_time
    ON manacommunity.health_observation (user_id, started_at DESC);

CREATE INDEX idx_health_observation_user_type_time
    ON manacommunity.health_observation (user_id, observation_type, started_at DESC);
