-- V285: Workforce Pool
CREATE TABLE mw_pool_listings (
  id                BIGSERIAL PRIMARY KEY,
  organization_id   BIGINT NOT NULL,
  title             VARCHAR(200) NOT NULL,
  description       TEXT,
  required_skills   TEXT,
  shift_date        DATE NOT NULL,
  shift_start       TIME NOT NULL,
  shift_end         TIME NOT NULL,
  location          VARCHAR(300),
  pay_rate          DECIMAL(10,2),
  pay_type          VARCHAR(20) DEFAULT 'HOURLY',
  max_workers       INT DEFAULT 1,
  accepted_count    INT DEFAULT 0,
  status            VARCHAR(20) DEFAULT 'OPEN',
  created_by        BIGINT NOT NULL,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pool_listing_date ON mw_pool_listings(shift_date);
CREATE INDEX idx_pool_listing_org ON mw_pool_listings(organization_id);
CREATE INDEX idx_pool_listing_status ON mw_pool_listings(status);

CREATE TABLE mw_pool_applications (
  id                BIGSERIAL PRIMARY KEY,
  listing_id        BIGINT NOT NULL,
  mana_user_id      BIGINT NOT NULL,
  passport_id       BIGINT,
  status            VARCHAR(20) DEFAULT 'APPLIED',
  worker_note       VARCHAR(500),
  employer_note     VARCHAR(500),
  applied_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  responded_at      TIMESTAMP,
  CONSTRAINT uq_pool_application UNIQUE (listing_id, mana_user_id),
  FOREIGN KEY (listing_id) REFERENCES mw_pool_listings(id)
);
CREATE INDEX idx_pool_app_user ON mw_pool_applications(mana_user_id);
CREATE INDEX idx_pool_app_listing ON mw_pool_applications(listing_id);

CREATE TABLE mw_engagement_contracts (
  id                BIGSERIAL PRIMARY KEY,
  application_id    BIGINT NOT NULL UNIQUE,
  terms_summary     TEXT,
  worker_consented  BOOLEAN DEFAULT FALSE,
  consented_at      TIMESTAMP,
  hours_worked      DECIMAL(5,2),
  amount_due        DECIMAL(10,2),
  status            VARCHAR(20) DEFAULT 'ACTIVE',
  completed_at      TIMESTAMP,
  FOREIGN KEY (application_id) REFERENCES mw_pool_applications(id)
);
