-- backfill rows seeded before updated_at column existed, then enforce NOT NULL
UPDATE manacommunity.community
    SET updated_at = created_at
    WHERE updated_at IS NULL;

ALTER TABLE manacommunity.community
    ALTER COLUMN updated_at SET NOT NULL;
