-- V336: Link commerce_order to unified payment_order table

ALTER TABLE commerce_order
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_commerce_order_payment_order
    ON commerce_order(payment_order_id);
