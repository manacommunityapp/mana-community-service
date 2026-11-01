-- ──────────────────────────────────────────────────────────────────────────────
-- V230: Widen sports_player_category columns
--   Prevents 'value too long' errors for 'SPORTS_ADMIN' (12 chars), 'MIXED_DOUBLES' (13 chars), etc.
-- ──────────────────────────────────────────────────────────────────────────────

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.tables 
        WHERE table_name = 'sports_player_category'
    ) THEN
        ALTER TABLE sports_player_category ALTER COLUMN type TYPE VARCHAR(50);
        ALTER TABLE sports_player_category ALTER COLUMN category_type TYPE VARCHAR(50);
        ALTER TABLE sports_player_category ALTER COLUMN gender TYPE VARCHAR(20);
    END IF;
END $$;

