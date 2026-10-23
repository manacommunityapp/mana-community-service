-- V77: token_number on event_pooja_slot_reservation, FK for reservation_id, updated_at on event_pooja_types

-- #12: Store token number on the reservation row so it can be returned on idempotency hits
ALTER TABLE event_pooja_slot_reservation
    ADD COLUMN IF NOT EXISTS token_number INTEGER DEFAULT 0;

-- #13: FK constraint from event_pooja_user_registrations.reservation_id → event_pooja_slot_reservation(id)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_epur_reservation_id'
          AND table_name       = 'event_pooja_user_registrations'
    ) THEN
        ALTER TABLE event_pooja_user_registrations
            ADD CONSTRAINT fk_epur_reservation_id
            FOREIGN KEY (reservation_id) REFERENCES event_pooja_slot_reservation(id) ON DELETE SET NULL;
    END IF;
END $$;

-- #25: Add updated_at to event_pooja_types (was only tracking created_at)
ALTER TABLE event_pooja_types
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE event_pooja_types SET updated_at = created_at WHERE updated_at IS NULL;
