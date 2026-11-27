-- V398: Mana Fresh & Core Engineering Standards Support
-- Creates distributed scheduler locking table and idempotency tracking table

CREATE TABLE IF NOT EXISTS manacommunity.scheduled_job_lock (
    job_name VARCHAR(64) PRIMARY KEY,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sched_lock_until ON manacommunity.scheduled_job_lock(locked_until);

CREATE TABLE IF NOT EXISTS manacommunity.fresh_idempotency_log (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    endpoint_path VARCHAR(255) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    response_status INT,
    response_body TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fresh_idem_status ON manacommunity.fresh_idempotency_log(status);
CREATE INDEX IF NOT EXISTS idx_fresh_idem_expires ON manacommunity.fresh_idempotency_log(expires_at);
