-- Audit trail for admin-created / override bookings
-- registration_source: SELF (normal user), ADMIN (created by admin), IMPORT (bulk import)
-- registered_by:       ID of the admin who created the booking on behalf of someone else
-- override_used:       true when adminOverride=true bypassed capacity / duplicate checks
-- override_reason:     free-text reason the admin provided for the override

ALTER TABLE event_booking_registrations
    ADD COLUMN IF NOT EXISTS registration_source VARCHAR(20) NOT NULL DEFAULT 'SELF',
    ADD COLUMN IF NOT EXISTS registered_by        BIGINT,
    ADD COLUMN IF NOT EXISTS override_used        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS override_reason      TEXT;

ALTER TABLE event_pooja_user_registrations
    ADD COLUMN IF NOT EXISTS registration_source VARCHAR(20) NOT NULL DEFAULT 'SELF',
    ADD COLUMN IF NOT EXISTS registered_by        BIGINT,
    ADD COLUMN IF NOT EXISTS override_used        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS override_reason      TEXT;
