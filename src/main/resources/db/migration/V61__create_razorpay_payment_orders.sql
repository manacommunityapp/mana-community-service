-- V61: Centralized Razorpay payment orders table.
-- Every paid business transaction (event registration, pooja, donation,
-- food order, auction, sports, etc.) creates one row here.
-- Amounts are stored in INR rupees; the service converts to paise when
-- calling the Razorpay API (×100).
-- Idempotency: webhook_event_id is unique and is checked before processing
-- duplicate Razorpay webhook deliveries.

CREATE TABLE IF NOT EXISTS razorpay_order (
    id                   BIGSERIAL PRIMARY KEY,

    -- Razorpay-assigned order ID (e.g. "order_Abc123xyz"). Unique per order.
    razorpay_order_id    VARCHAR(60)    NOT NULL,

    -- Razorpay payment ID populated after checkout success (e.g. "pay_Xyz456").
    razorpay_payment_id  VARCHAR(60),

    -- HMAC-SHA256 signature from Razorpay checkout, stored after server verification.
    razorpay_signature   VARCHAR(512),

    -- Amount in INR rupees (12 digits, 2 decimal places).
    amount               NUMERIC(12, 2) NOT NULL,
    currency             VARCHAR(5)     NOT NULL DEFAULT 'INR',

    -- Lifecycle status. Valid transitions:
    --   CREATED → PAID (via /verify or webhook payment.captured)
    --   CREATED → FAILED (via failed verify or webhook payment.failed)
    --   PAID    → REFUNDED (via webhook refund.processed)
    status               VARCHAR(20)    NOT NULL DEFAULT 'CREATED',

    -- What is being paid for. Examples: EVENT_REGISTRATION, POOJA, DONATION,
    -- FOOD_ORDER, AUCTION, SPORTS_REGISTRATION, CULTURAL_REGISTRATION, OTHER.
    reference_type       VARCHAR(60),

    -- PK of the referenced entity (CommunityEvent.id, PoojaBooking.id, etc.).
    reference_id         BIGINT,

    -- Short human-readable description shown in the Razorpay checkout modal.
    description          VARCHAR(500),

    -- JSON snapshot of Razorpay order notes forwarded to Razorpay (for audit).
    notes                TEXT,

    -- Owning user and community.
    user_id              BIGINT         NOT NULL REFERENCES app_user(id),
    community_id         BIGINT         NOT NULL REFERENCES community(id),

    -- Timestamps.
    paid_at              TIMESTAMP,
    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW(),

    -- Human-readable failure reason when status = FAILED.
    failure_reason       VARCHAR(500),

    -- Razorpay event.id from the webhook body. Checked before processing to
    -- skip duplicate webhook deliveries (idempotency key).
    webhook_event_id     VARCHAR(100),

    -- Optimistic locking version column.
    version              BIGINT         NOT NULL DEFAULT 0
);

-- Unique constraints enforced at DB level (not just Java).
ALTER TABLE razorpay_order
    ADD CONSTRAINT uq_razorpay_order_razorpay_id UNIQUE (razorpay_order_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_rzp_order_webhook_event
    ON razorpay_order (webhook_event_id)
    WHERE webhook_event_id IS NOT NULL;

-- Query-pattern indexes.
CREATE INDEX IF NOT EXISTS idx_rzp_order_payment_id
    ON razorpay_order (razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rzp_order_user_community
    ON razorpay_order (user_id, community_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_rzp_order_community_status
    ON razorpay_order (community_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_rzp_order_reference
    ON razorpay_order (reference_type, reference_id)
    WHERE reference_type IS NOT NULL;
