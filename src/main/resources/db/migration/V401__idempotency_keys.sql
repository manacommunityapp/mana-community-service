CREATE TABLE IF NOT EXISTS idempotency_key (
    key_value       VARCHAR(100)  PRIMARY KEY,
    request_path    VARCHAR(500),
    request_hash    VARCHAR(64),
    response_status INT,
    response_body   TEXT,
    community_id    BIGINT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_key(expires_at);
