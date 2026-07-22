-- FILE: db/sql/v1.0.0/33_marketplace_order.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- MARKETPLACE MODULE — Orders & Order Items
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS marketplace_order (
    id              BIGSERIAL       PRIMARY KEY,
    order_number    VARCHAR(30)     NOT NULL UNIQUE,
    buyer_id        BIGINT          NOT NULL REFERENCES app_user(id),
    seller_id       BIGINT          NOT NULL REFERENCES app_user(id),
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(12,2)   NOT NULL,
    notes           VARCHAR(500),
    delivery_address VARCHAR(200),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_item (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES marketplace_order(id) ON DELETE CASCADE,
    listing_id      BIGINT          NOT NULL REFERENCES listing(id),
    quantity        INT             NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12,2)   NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_buyer   ON marketplace_order (buyer_id, status);
CREATE INDEX IF NOT EXISTS idx_order_seller  ON marketplace_order (seller_id, status);
CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_item (order_id);
