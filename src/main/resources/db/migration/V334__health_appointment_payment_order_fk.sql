-- V334: Link health_appointment to unified payment_order table
ALTER TABLE manacommunity.health_appointment
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_health_appointment_payment_order
    ON manacommunity.health_appointment(payment_order_id);
