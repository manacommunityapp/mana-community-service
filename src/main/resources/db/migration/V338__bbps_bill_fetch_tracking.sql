-- V338: Track BBPS bill fetch and payment attempts.
-- payment_order_id is populated when /bill/pay creates a payment_order record.

CREATE TABLE IF NOT EXISTS bbps_bill_fetch (
    id                BIGSERIAL PRIMARY KEY,
    community_id      BIGINT      NOT NULL,
    user_id           BIGINT      NOT NULL,
    biller_id         VARCHAR(64) NOT NULL,
    biller_name       VARCHAR(200),
    consumer_id       VARCHAR(100) NOT NULL,
    bill_number       VARCHAR(100),
    bill_amount       NUMERIC(14, 2),
    due_date          DATE,
    status            VARCHAR(32) NOT NULL DEFAULT 'FETCHED',
    payment_order_id  BIGINT      REFERENCES payment_order(id),
    fetched_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bbps_bill_fetch_community ON bbps_bill_fetch(community_id);
CREATE INDEX IF NOT EXISTS idx_bbps_bill_fetch_user      ON bbps_bill_fetch(user_id);
CREATE INDEX IF NOT EXISTS idx_bbps_bill_fetch_payment   ON bbps_bill_fetch(payment_order_id);
