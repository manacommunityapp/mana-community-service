-- Add client idempotency_key deduplication on visitor_pass walk-ins
-- and covering composite index for flat-wide co-resident approval lookups.
ALTER TABLE visitor_pass
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uq_visitor_pass_community_idempotency
    ON visitor_pass (community_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_visitor_pass_comm_flat_status
    ON visitor_pass (community_id, flat_number, status, created_at DESC);
