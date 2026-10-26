-- Phase 22: Coupon Engine
CREATE TABLE IF NOT EXISTS commerce_coupon (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    coupon_code     VARCHAR(30)  NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     TEXT,
    coupon_type     VARCHAR(20)  NOT NULL DEFAULT 'PERCENT',  -- PERCENT FLAT FREE_DELIVERY
    value           NUMERIC(10,2) NOT NULL,
    min_order_amount NUMERIC(12,2),
    max_discount    NUMERIC(12,2),              -- cap for PERCENT type
    vendor_user_id  BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    category        VARCHAR(100),
    channel         VARCHAR(50),
    max_uses        INT,                        -- NULL = unlimited
    used_count      INT          NOT NULL DEFAULT 0,
    max_uses_per_user INT        DEFAULT 1,
    valid_from      TIMESTAMP    NOT NULL,
    valid_to        TIMESTAMP    NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by_id   BIGINT       REFERENCES app_user(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (community_id, coupon_code)
);

CREATE TABLE IF NOT EXISTS commerce_coupon_usage (
    id          BIGSERIAL PRIMARY KEY,
    coupon_id   BIGINT       NOT NULL REFERENCES commerce_coupon(id),
    user_id     BIGINT       NOT NULL REFERENCES app_user(id),
    order_id    BIGINT       REFERENCES commerce_order(id),
    order_number VARCHAR(40),
    discount_amount NUMERIC(12,2) NOT NULL,
    used_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coupon_community ON commerce_coupon (community_id, active);
CREATE INDEX IF NOT EXISTS idx_coupon_code      ON commerce_coupon (community_id, coupon_code);
CREATE INDEX IF NOT EXISTS idx_coupon_usage     ON commerce_coupon_usage (coupon_id, user_id);
