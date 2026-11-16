-- Unified payment order: single source of truth for all gateway-initiated payments.
-- Replaces the parallel cashfree_order / razorpay_order tables (those stay for now
-- as legacy — do NOT drop them in this migration).

CREATE TYPE payment_gateway AS ENUM ('CASHFREE', 'RAZORPAY', 'UPI_DIRECT', 'WALLET', 'MANUAL');
CREATE TYPE payment_order_status AS ENUM (
    'INITIATED', 'PENDING', 'SUCCESS', 'FAILED',
    'REFUND_PENDING', 'REFUNDED', 'PARTIALLY_REFUNDED', 'VOIDED', 'EXPIRED'
);
CREATE TYPE payment_reference_type AS ENUM (
    'MAINTENANCE_BILL', 'EVENT_REGISTRATION', 'POOJA_BOOKING',
    'SPORTS_REGISTRATION', 'FOOD_ORDER', 'MARKETPLACE_ORDER',
    'HEALTH_BOOKING', 'AMENITY_BOOKING', 'TRIP_SETTLEMENT',
    'BBPS_BILL', 'MANUAL'
);

CREATE TABLE payment_order (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    community_id        BIGINT NOT NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    gateway             payment_gateway NOT NULL,
    gateway_order_id    VARCHAR(128),
    gateway_payment_id  VARCHAR(128),
    reference_type      payment_reference_type NOT NULL,
    reference_id        BIGINT NOT NULL,
    payer_user_id       BIGINT NOT NULL,
    amount              NUMERIC(14,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              payment_order_status NOT NULL DEFAULT 'INITIATED',
    failure_reason      VARCHAR(500),
    refunded_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    ledger_entry_id     BIGINT,
    receipt_number      VARCHAR(100),
    webhook_received_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_order_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_payment_order_gateway     UNIQUE (gateway, gateway_order_id),
    CONSTRAINT chk_refunded_amount          CHECK (refunded_amount <= amount),
    CONSTRAINT chk_amount_positive          CHECK (amount > 0)
);

CREATE INDEX idx_payment_order_community   ON payment_order(community_id);
CREATE INDEX idx_payment_order_reference   ON payment_order(reference_type, reference_id);
CREATE INDEX idx_payment_order_status      ON payment_order(status);
CREATE INDEX idx_payment_order_payer       ON payment_order(payer_user_id);
CREATE INDEX idx_payment_order_created     ON payment_order(created_at DESC);

-- Link sports payments to the unified order
ALTER TABLE sports_tournament_player_payment
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);
