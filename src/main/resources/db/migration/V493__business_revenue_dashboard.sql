CREATE TABLE IF NOT EXISTS business_revenue_snapshots (
    id                    BIGSERIAL PRIMARY KEY,
    captured_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    period_start          DATE NOT NULL,
    period_end            DATE NOT NULL,
    snapshot_json         JSONB NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_biz_rev_snapshot_captured ON business_revenue_snapshots(captured_at DESC);
