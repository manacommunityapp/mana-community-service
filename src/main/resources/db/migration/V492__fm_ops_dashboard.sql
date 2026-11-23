-- FM / Committee Operations Dashboard support tables

-- FM-level alert thresholds per community
CREATE TABLE fm_ops_alert_configs (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT      NOT NULL,
    metric_key      VARCHAR(80) NOT NULL,
    warn_threshold  NUMERIC(12,2),
    crit_threshold  NUMERIC(12,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (community_id, metric_key)
);

-- Seed sensible defaults for the first community
INSERT INTO fm_ops_alert_configs (community_id, metric_key, warn_threshold, crit_threshold)
SELECT 1, t.key, t.warn, t.crit
FROM (VALUES
    ('COMPLAINT_OVERDUE_COUNT',  5,  10),
    ('DUES_OUTSTANDING_PCT',    15,  30),
    ('FUND_HEALTH_PCT',         70,  50),
    ('AMENITY_UTILIZATION_PCT', 80,  95)
) AS t(key, warn, crit)
ON CONFLICT DO NOTHING;

-- Daily FM ops snapshot (richer than community_manager_snapshots)
CREATE TABLE fm_ops_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT      NOT NULL,
    snapshot_json   TEXT        NOT NULL,
    captured_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_fm_ops_snapshot_community
    ON fm_ops_snapshots (community_id);
