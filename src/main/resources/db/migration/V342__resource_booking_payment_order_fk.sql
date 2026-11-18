-- V333: Link resource_booking to unified payment_order table
ALTER TABLE resource_booking
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_resource_booking_payment_order
    ON resource_booking(payment_order_id);
