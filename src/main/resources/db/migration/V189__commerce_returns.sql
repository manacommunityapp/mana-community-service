-- Phase 19: Return & Exchange
CREATE TABLE IF NOT EXISTS commerce_return (
    id                  BIGSERIAL PRIMARY KEY,
    return_code         VARCHAR(50)  NOT NULL UNIQUE,
    order_id            BIGINT       NOT NULL REFERENCES commerce_order(id),
    order_number        VARCHAR(40)  NOT NULL,
    customer_user_id    BIGINT       NOT NULL REFERENCES app_user(id),
    community_id        BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    return_type         VARCHAR(20)  NOT NULL DEFAULT 'RETURN',  -- RETURN EXCHANGE
    reason              VARCHAR(100) NOT NULL,
    description         TEXT,
    items_json          TEXT,
    refund_amount       NUMERIC(12,2),
    status              VARCHAR(30)  NOT NULL DEFAULT 'REQUESTED',
    -- REQUESTED APPROVED REJECTED ITEM_RECEIVED REFUNDED EXCHANGED
    admin_note          TEXT,
    resolved_by_id      BIGINT       REFERENCES app_user(id),
    resolved_at         TIMESTAMP,
    exchange_order_id   BIGINT       REFERENCES commerce_order(id),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_commerce_return_order    ON commerce_return (order_id);
CREATE INDEX IF NOT EXISTS idx_commerce_return_customer ON commerce_return (customer_user_id);
CREATE INDEX IF NOT EXISTS idx_commerce_return_community ON commerce_return (community_id, status);
