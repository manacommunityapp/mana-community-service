-- Phase 21: Bulk Orders
CREATE TABLE IF NOT EXISTS commerce_bulk_order (
    id                  BIGSERIAL PRIMARY KEY,
    bulk_order_code     VARCHAR(50)  NOT NULL UNIQUE,
    community_id        BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    customer_user_id    BIGINT       NOT NULL REFERENCES app_user(id),
    vendor_user_id      BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    items_json          TEXT         NOT NULL,   -- [{productName, sku, qty, unitPrice}]
    quoted_amount       NUMERIC(14,2),
    final_amount        NUMERIC(14,2),
    delivery_address    TEXT,
    requested_date      DATE,
    status              VARCHAR(30)  NOT NULL DEFAULT 'REQUESTED',
    -- REQUESTED QUOTED APPROVED FULFILLED CANCELLED
    customer_note       TEXT,
    admin_note          TEXT,
    vendor_note         TEXT,
    approved_by_id      BIGINT       REFERENCES app_user(id),
    approved_at         TIMESTAMP,
    fulfilled_order_id  BIGINT       REFERENCES commerce_order(id),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bulk_order_community ON commerce_bulk_order (community_id, status);
CREATE INDEX IF NOT EXISTS idx_bulk_order_customer  ON commerce_bulk_order (customer_user_id);
