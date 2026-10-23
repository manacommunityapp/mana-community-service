-- V80: Ensure all audit columns (created_at, updated_at, created_by, updated_by)
-- exist on event_ticket_categories.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'event_ticket_categories'
    ) THEN
        ALTER TABLE event_ticket_categories
            ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
            ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
            ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS community_id BIGINT;
    END IF;

    -- Also check manacommunity schema explicitly if schema-qualified
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'manacommunity' AND table_name = 'event_ticket_categories'
    ) THEN
        ALTER TABLE manacommunity.event_ticket_categories
            ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
            ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
            ADD COLUMN IF NOT EXISTS created_by  BIGINT       NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS updated_by  BIGINT       NOT NULL DEFAULT 0,
            ADD COLUMN IF NOT EXISTS community_id BIGINT;
    END IF;
END $$;
