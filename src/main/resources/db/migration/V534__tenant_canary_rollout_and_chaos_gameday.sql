-- Progressive Tenant Canary Feature Flags, Shadow Billing Diffs & Gate Chaos GameDay Ledger

CREATE TABLE IF NOT EXISTS tenant_feature_flags (
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(300),
    ring_stage VARCHAR(32) NOT NULL DEFAULT 'INTERNAL_PILOT',
    rollout_bps INT NOT NULL DEFAULT 0,
    kill_switch BOOLEAN NOT NULL DEFAULT FALSE,
    shadow_mode BOOLEAN NOT NULL DEFAULT FALSE,
    max_p99_latency_ms INT NOT NULL DEFAULT 300,
    max_error_rate_bps INT NOT NULL DEFAULT 50,
    block_rush_hours BOOLEAN NOT NULL DEFAULT TRUE,
    rollback_reason VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS canary_shadow_billing_diffs (
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(100) NOT NULL,
    community_id BIGINT NOT NULL,
    invoice_ref VARCHAR(64) NOT NULL,
    legacy_total NUMERIC(12, 2) NOT NULL,
    canary_total NUMERIC(12, 2) NOT NULL,
    delta_amount NUMERIC(12, 2) NOT NULL,
    matched BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shadow_diff_flag_comm
    ON canary_shadow_billing_diffs (flag_key, community_id);

CREATE TABLE IF NOT EXISTS chaos_gameday_experiments (
    id BIGSERIAL PRIMARY KEY,
    experiment_code VARCHAR(64) NOT NULL UNIQUE,
    community_id BIGINT NOT NULL,
    fault_type VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RUNNING',
    max_duration_sec INT NOT NULL DEFAULT 600,
    sos_probe_failed BOOLEAN NOT NULL DEFAULT FALSE,
    gate_degraded_ok BOOLEAN NOT NULL DEFAULT TRUE,
    abort_reason VARCHAR(255),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);
