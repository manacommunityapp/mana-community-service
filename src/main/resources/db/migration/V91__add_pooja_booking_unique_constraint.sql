-- Enforce one active booking per user per schedule slot at the DB level.
-- Application-level guards (existsByUserIdAndScheduleIdAndStatusNot) have a race window;
-- this partial unique index closes it completely.
--
-- The index is partial: it only covers rows where status is active (not CANCELLED / EXPIRED)
-- and both columns are non-null, so legacy rows without schedule_id are unaffected.

-- Step 1: de-duplicate any existing active doubles — keep the earliest booking per pair.
DELETE FROM event_pooja_user_registrations
WHERE id NOT IN (
    SELECT MIN(id)
    FROM event_pooja_user_registrations
    WHERE user_id    IS NOT NULL
      AND schedule_id IS NOT NULL
      AND status NOT IN ('CANCELLED', 'EXPIRED')
    GROUP BY user_id, schedule_id
)
AND user_id     IS NOT NULL
AND schedule_id IS NOT NULL
AND status NOT IN ('CANCELLED', 'EXPIRED');

-- Step 2: create the index.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pooja_booking_active
    ON event_pooja_user_registrations (user_id, schedule_id)
    WHERE status NOT IN ('CANCELLED', 'EXPIRED')
      AND user_id    IS NOT NULL
      AND schedule_id IS NOT NULL;
