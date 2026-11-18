-- Add paymentStatus column to health_appointment
ALTER TABLE health_appointment
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30);

-- Add paymentStatus column to community_trip_booking
ALTER TABLE community_trip_booking
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_health_appt_payment_status
    ON health_appointment(payment_status) WHERE payment_status IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_trip_booking_payment_status
    ON community_trip_booking(payment_status) WHERE payment_status IS NOT NULL;
