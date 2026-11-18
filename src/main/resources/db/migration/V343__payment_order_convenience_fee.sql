ALTER TABLE payment_order
    ADD COLUMN IF NOT EXISTS convenience_fee_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS gross_amount           NUMERIC(14,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN payment_order.convenience_fee_amount IS 'Platform convenience fee charged on this order';
COMMENT ON COLUMN payment_order.gross_amount IS 'amount + convenience_fee_amount; this is what the payer actually pays';
