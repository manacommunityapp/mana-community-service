-- V337: Link community_trip_booking to unified payment_order table (external trip fees only).
-- Internal split-settlement transfers are handled via the CFBOS ledger directly.

ALTER TABLE manacommunity.community_trip_booking
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_trip_booking_payment_order
    ON manacommunity.community_trip_booking(payment_order_id);
