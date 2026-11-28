-- V538: Cloud FinOps, S3 Lifecycle/Compression Telemetry & Cost-Aware Telecom Delivery Router

CREATE SCHEMA IF NOT EXISTS finops;

CREATE TABLE IF NOT EXISTS finops.resident_channel_profiles (
    resident_id          VARCHAR(100) PRIMARY KEY,
    society_id           BIGINT NOT NULL,
    ws_last_seen_at      TIMESTAMPTZ,
    fcm_p50_ms           INTEGER NOT NULL DEFAULT 1800,
    fcm_p90_ms           INTEGER NOT NULL DEFAULT 3200,
    fcm_ack_rate_7d      DOUBLE PRECISION NOT NULL DEFAULT 0.85,
    wa_p50_ms            INTEGER NOT NULL DEFAULT 4200,
    wa_ack_rate_7d       DOUBLE PRECISION NOT NULL DEFAULT 0.80,
    wa_opt_in            BOOLEAN NOT NULL DEFAULT TRUE,
    ivr_ack_rate_7d      DOUBLE PRECISION NOT NULL DEFAULT 0.92,
    oem_aggressive_doze  BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_channel    VARCHAR(30) NOT NULL DEFAULT 'FCM_PUSH',
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS finops.delivery_routing_events (
    alert_id                UUID PRIMARY KEY,
    society_id              BIGINT NOT NULL,
    resident_id             VARCHAR(100) NOT NULL,
    visitor_type            VARCHAR(50) NOT NULL,
    sla_ms                  INTEGER NOT NULL DEFAULT 15000,
    selected_initial_channel VARCHAR(30) NOT NULL,
    current_channel         VARCHAR(30) NOT NULL,
    escalation_path_json    JSONB NOT NULL,
    winning_channel         VARCHAR(30),
    resolved_latency_ms     INTEGER,
    sla_met                 BOOLEAN,
    total_cost_paise        INTEGER NOT NULL DEFAULT 0,
    baseline_ivr_cost_paise INTEGER NOT NULL DEFAULT 40,
    savings_paise           INTEGER NOT NULL DEFAULT 40,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    routing_reason          VARCHAR(400),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at             TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS finops.storage_tier_policies (
    asset_class           VARCHAR(50) PRIMARY KEY,
    target_format         VARCHAR(20) NOT NULL,
    hot_tier              VARCHAR(40) NOT NULL,
    hot_days              INTEGER NOT NULL,
    warm_tier             VARCHAR(40) NOT NULL,
    warm_days             INTEGER NOT NULL,
    archive_tier          VARCHAR(40) NOT NULL,
    retention_days        INTEGER NOT NULL,
    min_billable_kb       INTEGER NOT NULL DEFAULT 128,
    notes                 TEXT
);

CREATE TABLE IF NOT EXISTS finops.media_compression_audit (
    audit_id          UUID PRIMARY KEY,
    society_id        BIGINT NOT NULL,
    asset_class       VARCHAR(50) NOT NULL,
    original_bytes    BIGINT NOT NULL,
    compressed_bytes  BIGINT NOT NULL,
    format            VARCHAR(20) NOT NULL,
    storage_tier      VARCHAR(40) NOT NULL DEFAULT 'S3_STANDARD',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_finops_routing_society_created ON finops.delivery_routing_events (society_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_finops_media_society_class ON finops.media_compression_audit (society_id, asset_class, created_at DESC);
