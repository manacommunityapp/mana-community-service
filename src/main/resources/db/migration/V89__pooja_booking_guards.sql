-- V89: Backend guard gaps -- duplicate booking DB-level constraints (G-4)
--
-- G-4a: Prevent a user from having more than one non-cancelled registration
--       for the same Pooja schedule row.
--       Uses a partial index (WHERE status != 'CANCELLED') so cancelled rows
--       are excluded and a user can re-book after cancelling.
--
-- G-4b: Prevent a user from having more than one active (RESERVED or CONFIRMED)
--       reservation for the same schedule slot -- adds a DB-level race-condition
--       guard on top of the pessimistic-lock application check.
--
-- Note: Partial index syntax (WHERE clause) is PostgreSQL-specific.
--       On MySQL/MariaDB, remove the WHERE clause and rely solely on the
--       application-level check in EventPoojaUserRegistrationServiceImpl.

-- G-4a: event_pooja_user_registrations
-- Only enforce uniqueness on rows that are NOT cancelled.
-- Allows a user to cancel and then re-book the same slot.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pooja_reg_user_schedule_active
    ON event_pooja_user_registrations (user_id, schedule_id)
    WHERE status <> 'CANCELLED';

-- G-4b: event_pooja_slot_reservation
-- Enforce uniqueness only on rows that are RESERVED or CONFIRMED.
-- EXPIRED / CANCELLED rows are excluded so the seat can be re-reserved.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pooja_reservation_user_schedule_active
    ON event_pooja_slot_reservation (user_id, schedule_id)
    WHERE status IN ('RESERVED', 'CONFIRMED');
