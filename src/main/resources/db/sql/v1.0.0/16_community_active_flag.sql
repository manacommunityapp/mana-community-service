-- FILE: db/sql/v1.0.0/16_community_active_flag.sql
-- Soft-delete flag for communities (Community.active). Deleting a community sets
-- active=false instead of removing the row, preserving references.
-- Required for the prod profile (ddl-auto: validate). local=create / dev=update auto-add it.
-- Idempotent: safe to re-run.

ALTER TABLE community
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

-- Backfill any pre-existing rows so active-only queries include them.
UPDATE community SET active = TRUE WHERE active IS NULL;
