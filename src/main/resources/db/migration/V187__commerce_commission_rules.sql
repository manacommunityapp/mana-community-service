-- V187: Commission rules engine for configurable platform fee per vendor / category

CREATE TABLE IF NOT EXISTS commerce_commission_rule (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT        NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    rule_name           VARCHAR(100)  NOT NULL,
    vendor_user_id      BIGINT        REFERENCES app_user(id) ON DELETE SET NULL,
    category            VARCHAR(100),
    channel             VARCHAR(50),
    fee_percent         NUMERIC(6,4)  NOT NULL DEFAULT 2.5000,
    gst_on_fee          BOOLEAN       NOT NULL DEFAULT FALSE,
    tds_percent         NUMERIC(6,4),
    priority            INT           NOT NULL DEFAULT 100,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    notes               TEXT,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_commission_rule_community  ON commerce_commission_rule (community_id, active);
CREATE INDEX IF NOT EXISTS idx_commission_rule_vendor     ON commerce_commission_rule (vendor_user_id);
