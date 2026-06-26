-- FILE: db/sql/v1.0.0/15_sports_event_contact_name.sql
-- Adds the primary contact name to sports_event (SportsEvent.contactName).
-- Required for the prod profile (ddl-auto: validate). local=create / dev=update auto-add it.
-- Idempotent: safe to re-run.

ALTER TABLE sports_event
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255);

-- The full event editor stores its contact details on the tournament record.
ALTER TABLE tournament
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255);
