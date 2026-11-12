-- Per-org forecasting configuration
CREATE TABLE mw_forecast_configs (
  id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id       BIGINT NOT NULL UNIQUE,
  horizon_weeks         INT DEFAULT 4,
  min_workers_per_shift INT DEFAULT 1,
  peak_day_multiplier   DECIMAL(4,2) DEFAULT 1.5,
  source_modules        VARCHAR(200) DEFAULT 'POOL,JOBS',
  auto_snapshot_enabled BOOLEAN DEFAULT TRUE,
  created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Weekly demand snapshots
CREATE TABLE mw_forecast_snapshots (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id     BIGINT NOT NULL,
  week_start_date     DATE NOT NULL,
  source_module       VARCHAR(50) NOT NULL,
  demand_units        INT NOT NULL DEFAULT 0,
  fulfilled_units     INT NOT NULL DEFAULT 0,
  gap_units           INT GENERATED ALWAYS AS (demand_units - fulfilled_units) STORED,
  avg_pay_rate        DECIMAL(8,2),
  worker_count        INT DEFAULT 0,
  notes               TEXT,
  snapshot_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_snapshot_week_module (organization_id, week_start_date, source_module),
  INDEX idx_snapshot_org_week (organization_id, week_start_date)
);

-- Forecasted demand projections (computed from snapshots)
CREATE TABLE mw_forecast_projections (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id     BIGINT NOT NULL,
  projected_week      DATE NOT NULL,
  source_module       VARCHAR(50) NOT NULL,
  projected_demand    INT NOT NULL,
  confidence_pct      INT DEFAULT 70,
  generated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_projection_week_module (organization_id, projected_week, source_module),
  INDEX idx_projection_org (organization_id)
);
