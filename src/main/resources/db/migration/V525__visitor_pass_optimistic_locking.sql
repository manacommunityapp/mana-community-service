-- Add JPA optimistic locking version column to visitor_pass to eliminate lost updates
-- during concurrent resident Approve / Deny / Leave-at-Gate actions across pods.
ALTER TABLE visitor_pass
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
