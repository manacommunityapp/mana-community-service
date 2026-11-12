-- Integration config per org (which Mana modules are linked)
CREATE TABLE mw_integration_configs (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id   BIGINT NOT NULL UNIQUE,
  fresh_enabled     BOOLEAN DEFAULT FALSE,
  homes_enabled     BOOLEAN DEFAULT FALSE,
  sports_enabled    BOOLEAN DEFAULT FALSE,
  base_delivery_pay DECIMAL(8,2) DEFAULT 0.00,
  per_km_rate       DECIMAL(6,2) DEFAULT 0.00,
  on_time_bonus     DECIMAL(6,2) DEFAULT 0.00,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Mana Fresh delivery assignments (linked to mw_operational_assignments)
CREATE TABLE mw_fresh_assignments (
  id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
  operational_assignment_id BIGINT NOT NULL UNIQUE,
  fresh_order_id            VARCHAR(100) NOT NULL,
  pickup_location           VARCHAR(300),
  drop_location             VARCHAR(300),
  estimated_km              DECIMAL(6,2),
  actual_km                 DECIMAL(6,2),
  on_time                   BOOLEAN,
  base_pay                  DECIMAL(8,2),
  km_bonus                  DECIMAL(8,2) DEFAULT 0.00,
  time_bonus                DECIMAL(8,2) DEFAULT 0.00,
  total_incentive           DECIMAL(8,2) GENERATED ALWAYS AS (base_pay + km_bonus + time_bonus) STORED,
  created_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (operational_assignment_id) REFERENCES mw_operational_assignments(id),
  INDEX idx_fresh_order (fresh_order_id)
);
