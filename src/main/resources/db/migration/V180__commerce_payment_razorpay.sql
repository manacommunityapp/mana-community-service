-- Phase 5: Razorpay payment integration for commerce orders
-- Extends commerce_payment with Razorpay-specific tracking fields.

ALTER TABLE commerce_payment
    ADD COLUMN IF NOT EXISTS razorpay_order_id   VARCHAR(80),
    ADD COLUMN IF NOT EXISTS razorpay_payment_id  VARCHAR(80),
    ADD COLUMN IF NOT EXISTS failure_reason       VARCHAR(300),
    ADD COLUMN IF NOT EXISTS captured_at          TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS idx_cp_razorpay_order_id
    ON commerce_payment(razorpay_order_id)
    WHERE razorpay_order_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_cp_razorpay_payment_id
    ON commerce_payment(razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL;

-- Update commerce_order payment_status allowed values comment (no constraint change needed,
-- existing status column is an unconstrained VARCHAR; PAYMENT_AUTHORIZED is already in the enum)
