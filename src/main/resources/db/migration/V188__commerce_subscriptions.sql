-- Phase 18: Subscription / Recurring Orders
CREATE TABLE IF NOT EXISTS commerce_subscription (
    id                  BIGSERIAL PRIMARY KEY,
    subscription_code   VARCHAR(50)  NOT NULL UNIQUE,
    community_id        BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    customer_user_id    BIGINT       NOT NULL REFERENCES app_user(id),
    product_id          BIGINT       REFERENCES commerce_products(id) ON DELETE SET NULL,
    product_name        VARCHAR(255) NOT NULL,
    sku                 VARCHAR(100),
    seller_id           BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    seller_name         VARCHAR(150),
    unit_price          NUMERIC(12,2) NOT NULL,
    quantity            INT          NOT NULL DEFAULT 1,
    channel             VARCHAR(50)  NOT NULL DEFAULT 'MARKETPLACE',
    frequency           VARCHAR(20)  NOT NULL DEFAULT 'WEEKLY',   -- DAILY WEEKLY FORTNIGHTLY MONTHLY
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE PAUSED CANCELLED
    next_run_date       DATE         NOT NULL,
    last_run_date       DATE,
    total_cycles        INT,                                       -- NULL = infinite
    completed_cycles    INT          NOT NULL DEFAULT 0,
    delivery_address    TEXT,
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_commerce_sub_community ON commerce_subscription (community_id, status);
CREATE INDEX IF NOT EXISTS idx_commerce_sub_customer  ON commerce_subscription (customer_user_id);
CREATE INDEX IF NOT EXISTS idx_commerce_sub_next_run  ON commerce_subscription (next_run_date, status);
