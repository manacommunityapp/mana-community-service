-- V332: Link event_booking_registrations to unified payment_order table
ALTER TABLE event_booking_registrations
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_event_reg_payment_order
    ON event_booking_registrations(payment_order_id);
