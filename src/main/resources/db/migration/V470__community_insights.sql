-- Daily snapshot of key community metrics (append-only time series)
CREATE TABLE community_metrics_snapshots (
  id               BIGSERIAL PRIMARY KEY,
  community_id     BIGINT NOT NULL,
  snapshot_date    DATE NOT NULL,
  total_dues_outstanding NUMERIC(15,2),
  collection_rate  NUMERIC(5,2),
  avg_payment_delay_days NUMERIC(5,1),
  open_complaints  INT,
  avg_resolution_hours NUMERIC(7,1),
  overdue_maintenance  INT,
  active_residents_pct NUMERIC(5,2),
  events_this_month    INT,
  votes_cast_this_month INT,
  gate_entries_today   INT,
  unrecognised_entries INT,
  water_kl         NUMERIC(10,2),
  electricity_kwh  NUMERIC(10,2),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(community_id, snapshot_date)
);

-- Rule-based insight cards surfaced on the ops dashboard
CREATE TABLE community_insights (
  id               BIGSERIAL PRIMARY KEY,
  community_id     BIGINT NOT NULL,
  insight_type     VARCHAR(50) NOT NULL,
  severity         VARCHAR(20) NOT NULL DEFAULT 'INFO',
  title            VARCHAR(300) NOT NULL,
  description      TEXT NOT NULL,
  recommendation   TEXT,
  metric_value     NUMERIC(15,2),
  metric_unit      VARCHAR(50),
  reference_period VARCHAR(50),
  is_dismissed     BOOLEAN NOT NULL DEFAULT FALSE,
  dismissed_by     BIGINT,
  generated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at       TIMESTAMPTZ
);

CREATE INDEX idx_metrics_community ON community_metrics_snapshots(community_id, snapshot_date DESC);
CREATE INDEX idx_insights_active ON community_insights(community_id, severity, generated_at DESC)
  WHERE is_dismissed = false;
