-- Platform-level metrics time series (Mana platform team only)
CREATE TABLE platform_metrics_history (
  id               BIGSERIAL PRIMARY KEY,
  metric_type      VARCHAR(80) NOT NULL,
  value            NUMERIC(15,4) NOT NULL,
  unit             VARCHAR(30),
  recorded_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Platform incident / alert log
CREATE TABLE platform_alerts (
  id               BIGSERIAL PRIMARY KEY,
  alert_type       VARCHAR(80) NOT NULL,
  severity         VARCHAR(20) NOT NULL,
  title            VARCHAR(300) NOT NULL,
  description      TEXT,
  community_id     BIGINT,
  metric_value     NUMERIC(15,4),
  threshold_value  NUMERIC(15,4),
  resolved_at      TIMESTAMPTZ,
  resolved_by      BIGINT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- CI/CD deploy event log
CREATE TABLE platform_deploy_events (
  id               BIGSERIAL PRIMARY KEY,
  service_name     VARCHAR(100) NOT NULL,
  version          VARCHAR(100) NOT NULL,
  environment      VARCHAR(30) NOT NULL DEFAULT 'PRODUCTION',
  status           VARCHAR(30) NOT NULL,
  deployed_by      VARCHAR(200),
  commit_sha       VARCHAR(50),
  release_notes    TEXT,
  started_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at      TIMESTAMPTZ
);

CREATE INDEX idx_platform_metrics_type ON platform_metrics_history(metric_type, recorded_at DESC);
CREATE INDEX idx_platform_alerts_open  ON platform_alerts(severity, created_at DESC) WHERE resolved_at IS NULL;
CREATE INDEX idx_deploy_events_svc     ON platform_deploy_events(service_name, started_at DESC);
