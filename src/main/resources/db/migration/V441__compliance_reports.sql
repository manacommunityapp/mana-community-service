CREATE TABLE compliance_report_configs (
  id           BIGSERIAL PRIMARY KEY,
  community_id BIGINT NOT NULL,
  report_type  VARCHAR(50) NOT NULL,
  frequency    VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
  schedule_cron VARCHAR(100),
  recipients   JSONB,
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(community_id, report_type)
);

CREATE TABLE generated_reports (
  id           BIGSERIAL PRIMARY KEY,
  community_id BIGINT NOT NULL,
  report_type  VARCHAR(50) NOT NULL,
  report_period VARCHAR(30) NOT NULL,
  file_url     VARCHAR(500),
  file_format  VARCHAR(20) DEFAULT 'PDF',
  generated_by BIGINT,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  row_count    INT,
  total_amount NUMERIC(15,2),
  metadata     JSONB
);

CREATE INDEX idx_reports_community ON generated_reports(community_id, report_type, generated_at DESC);
