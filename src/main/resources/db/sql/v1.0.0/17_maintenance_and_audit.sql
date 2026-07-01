-- FILE: db/sql/v1.0.0/17_maintenance_and_audit.sql
-- Table definitions for Maintenance records and Asset Audit logs.

CREATE TABLE IF NOT EXISTS maintenance_records (
    id               BIGSERIAL      PRIMARY KEY,
    asset_id         BIGINT         NOT NULL,
    maintenance_date DATE           NOT NULL,
    type             VARCHAR(40)    NOT NULL,
    description      VARCHAR(255),
    cost             DECIMAL(12, 2) DEFAULT 0.00,
    performed_by     VARCHAR(120),
    status           VARCHAR(30)    NOT NULL DEFAULT 'SCHEDULED',
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_asset FOREIGN KEY (asset_id) REFERENCES inventory_items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS asset_audit_logs (
    id                BIGSERIAL    PRIMARY KEY,
    asset_id          BIGINT       NOT NULL,
    audited_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audited_by        VARCHAR(120),
    expected_status   VARCHAR(30),
    actual_status     VARCHAR(30),
    expected_quantity INT,
    actual_quantity   INT,
    variance          INT,
    notes             VARCHAR(255),
    CONSTRAINT fk_audit_asset FOREIGN KEY (asset_id) REFERENCES inventory_items(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_maintenance_asset ON maintenance_records (asset_id, maintenance_date);
CREATE INDEX IF NOT EXISTS idx_audit_asset_time  ON asset_audit_logs (asset_id, audited_at);
