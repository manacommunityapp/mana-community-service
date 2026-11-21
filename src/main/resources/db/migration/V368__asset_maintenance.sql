CREATE TYPE asset_category AS ENUM ('LIFT','GENERATOR','PUMP','CCTV','FIRE_SAFETY','ELECTRICAL','PLUMBING','HVAC','GATE','OTHER');
CREATE TYPE asset_status AS ENUM ('OPERATIONAL','UNDER_MAINTENANCE','OUT_OF_ORDER','DECOMMISSIONED');
CREATE TYPE maintenance_frequency AS ENUM ('WEEKLY','MONTHLY','QUARTERLY','HALF_YEARLY','YEARLY');
CREATE TYPE maintenance_log_status AS ENUM ('SCHEDULED','IN_PROGRESS','COMPLETED','SKIPPED','OVERDUE');

CREATE TABLE community_asset (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    category        asset_category NOT NULL,
    model_number    VARCHAR(100),
    serial_number   VARCHAR(100),
    location        VARCHAR(255),
    purchase_date   DATE,
    warranty_expiry DATE,
    amc_vendor      VARCHAR(255),
    amc_expiry      DATE,
    status          asset_status NOT NULL DEFAULT 'OPERATIONAL',
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE asset_maintenance_schedule (
    id              BIGSERIAL PRIMARY KEY,
    asset_id        BIGINT NOT NULL REFERENCES community_asset(id),
    task_name       VARCHAR(255) NOT NULL,
    frequency       maintenance_frequency NOT NULL,
    last_done       DATE,
    next_due        DATE NOT NULL,
    assigned_vendor VARCHAR(255),
    estimated_cost  NUMERIC(12,2),
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE asset_maintenance_log (
    id               BIGSERIAL PRIMARY KEY,
    asset_id         BIGINT NOT NULL REFERENCES community_asset(id),
    schedule_id      BIGINT REFERENCES asset_maintenance_schedule(id),
    task_description TEXT NOT NULL,
    status           maintenance_log_status NOT NULL DEFAULT 'SCHEDULED',
    scheduled_date   DATE NOT NULL,
    completed_date   DATE,
    performed_by     VARCHAR(255),
    cost_actual      NUMERIC(12,2),
    notes            TEXT,
    reported_by      BIGINT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_asset_comm ON community_asset(community_id, status);
CREATE INDEX idx_ams_asset  ON asset_maintenance_schedule(asset_id, next_due);
CREATE INDEX idx_aml_asset  ON asset_maintenance_log(asset_id, scheduled_date);
