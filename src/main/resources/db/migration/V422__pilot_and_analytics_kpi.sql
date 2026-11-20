-- V422__pilot_and_analytics_kpi.sql
-- Pilot community configuration and product analytics event log for pilot measurement

CREATE TABLE IF NOT EXISTS pilot_community_config (
    community_id BIGINT PRIMARY KEY REFERENCES community(id) ON DELETE CASCADE,
    is_pilot     BOOLEAN NOT NULL DEFAULT FALSE,
    cohort       VARCHAR(50),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_analytics_event (
    id           BIGSERIAL PRIMARY KEY,
    event_name   VARCHAR(100) NOT NULL,
    community_id BIGINT,
    user_hash    VARCHAR(64),
    cohort       VARCHAR(50),
    session_id   VARCHAR(64),
    props_json   TEXT,
    occurred_at  TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pae_comm_occurred  ON product_analytics_event(community_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_pae_event_occurred ON product_analytics_event(event_name, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_pae_cohort         ON product_analytics_event(cohort);
