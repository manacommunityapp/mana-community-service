-- V124: Sports event table improvements
-- 1. Normalize format column into sports_event_format join table
-- 2. Consolidate auction/auction_enabled into auction_enabled only
-- 3. Widen name column to 150 chars
-- 4. Add index on community_id
-- 5. Constrain tournament_level to 50 chars

-- 1. Create the normalized format table
CREATE TABLE IF NOT EXISTS manacommunity.sports_event_format (
    event_id BIGINT NOT NULL,
    format   VARCHAR(30) NOT NULL,
    CONSTRAINT fk_event_format_event FOREIGN KEY (event_id)
        REFERENCES manacommunity.sports_event(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_event_format_event_id ON manacommunity.sports_event_format(event_id);
CREATE INDEX IF NOT EXISTS idx_event_format_value ON manacommunity.sports_event_format(format);

-- Migrate comma-separated data from the old format column
DO $$
DECLARE
    rec RECORD;
    fmt TEXT;
BEGIN
    FOR rec IN SELECT id, format FROM manacommunity.sports_event WHERE format IS NOT NULL AND format <> ''
    LOOP
        FOREACH fmt IN ARRAY string_to_array(rec.format, ',')
        LOOP
            INSERT INTO manacommunity.sports_event_format (event_id, format)
            VALUES (rec.id, TRIM(fmt))
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- Drop the old format column
ALTER TABLE manacommunity.sports_event DROP COLUMN IF EXISTS format;

-- 2. Consolidate auction columns: keep auction_enabled, drop auction
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'manacommunity' AND table_name = 'sports_event' AND column_name = 'auction')
    THEN
        UPDATE manacommunity.sports_event
        SET auction_enabled = COALESCE(auction_enabled, auction, FALSE)
        WHERE auction_enabled IS NULL AND auction IS NOT NULL;

        ALTER TABLE manacommunity.sports_event DROP COLUMN auction;
    END IF;
END $$;

-- 3. Widen name column from 50 to 150
ALTER TABLE manacommunity.sports_event ALTER COLUMN name TYPE VARCHAR(150);

-- 4. Add index on community_id for faster community-scoped queries
CREATE INDEX IF NOT EXISTS idx_sports_event_community_id ON manacommunity.sports_event(community_id);

-- 5. Constrain tournament_level (was unconstrained varchar(255))
ALTER TABLE manacommunity.sports_event ALTER COLUMN tournament_level TYPE VARCHAR(50);
