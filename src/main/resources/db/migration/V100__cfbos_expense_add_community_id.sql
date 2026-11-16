-- Add community_id to cfbos_expense for multi-tenant isolation.
-- Existing rows are backfilled with community_id = 1 (default community).
-- The NOT NULL constraint is added after backfill.

ALTER TABLE cfbos_expense ADD COLUMN IF NOT EXISTS community_id BIGINT;

UPDATE cfbos_expense SET community_id = 1 WHERE community_id IS NULL;

ALTER TABLE cfbos_expense ALTER COLUMN community_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cfbos_expense_community_id ON cfbos_expense (community_id);
