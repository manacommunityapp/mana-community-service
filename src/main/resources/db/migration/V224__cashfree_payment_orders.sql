-- ────────────────────────────────────────────────────────────────
-- V224 — Cashfree Payment Orders (renumbered from V100 to fix duplicate)
-- Centralised payment table for all Cashfree PG transactions.
-- Shared by sports, events, venue booking, marketplace, etc.
-- ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cashfree_order (
    id                  BIGSERIAL PRIMARY KEY,
    mana_order_id       VARCHAR(50)     NOT NULL UNIQUE,
    cf_order_id         VARCHAR(40),
    payment_session_id  VARCHAR(512),
    cf_payment_id       VARCHAR(100),
    amount              NUMERIC(12, 2)  NOT NULL,
    currency            VARCHAR(5)      NOT NULL DEFAULT 'INR',
    status              VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    reference_type      VARCHAR(60),
    reference_id        BIGINT,
    description         VARCHAR(500),
    payment_method      VARCHAR(50),
    failure_reason      VARCHAR(500),
    webhook_event_id    VARCHAR(100),
    user_id             BIGINT          NOT NULL REFERENCES app_user(id),
    community_id        BIGINT          NOT NULL REFERENCES community(id),
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cf_order_user_community
    ON cashfree_order (user_id, community_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cf_order_community_status
    ON cashfree_order (community_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cf_order_reference
    ON cashfree_order (reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_cf_order_mana_order_id
    ON cashfree_order (mana_order_id);

COMMENT ON TABLE  cashfree_order                    IS 'Cashfree PG payment orders — one row per checkout session';
COMMENT ON COLUMN cashfree_order.mana_order_id      IS 'Mana-generated order ID sent to Cashfree as order_id (MANA-{ms}-{userId})';
COMMENT ON COLUMN cashfree_order.cf_order_id        IS 'Cashfree cf_order_id from order creation response';
COMMENT ON COLUMN cashfree_order.payment_session_id IS 'Cashfree payment_session_id; used by mobile/web SDK to open checkout';
COMMENT ON COLUMN cashfree_order.cf_payment_id      IS 'Cashfree payment reference, populated after PAID';
COMMENT ON COLUMN cashfree_order.reference_type     IS 'Business module discriminator (SPORTS_REGISTRATION, EVENT_TICKET, etc.)';
COMMENT ON COLUMN cashfree_order.webhook_event_id   IS 'Deduplication key derived from webhook event for idempotent processing';
