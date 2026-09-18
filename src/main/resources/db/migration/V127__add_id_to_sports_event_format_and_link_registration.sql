-- =====================================================================
-- V127: Add id PRIMARY KEY to sports_event_format and link format_id
--       in sports_event_registration.
--
-- Single DO block: PK and FK created together so Postgres constraint
-- visibility is guaranteed. Column check and PK check are SEPARATE
-- (handles the case where id column exists but PK was never added).
-- =====================================================================

DO $$
DECLARE
    seq_name TEXT;
BEGIN
    -- ── 1. Add id column if it does not exist ─────────────────────────
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE  table_schema = 'manacommunity'
          AND  table_name   = 'sports_event_format'
          AND  column_name  = 'id'
    ) THEN
        ALTER TABLE manacommunity.sports_event_format ADD COLUMN id BIGINT;
    END IF;

    -- ── 2. Attach sequence if not already attached ────────────────────
    seq_name := pg_get_serial_sequence('manacommunity.sports_event_format', 'id');
    IF seq_name IS NULL THEN
        CREATE SEQUENCE IF NOT EXISTS manacommunity.sports_event_format_id_seq;
        ALTER TABLE manacommunity.sports_event_format
            ALTER COLUMN id SET DEFAULT nextval('manacommunity.sports_event_format_id_seq');
        ALTER SEQUENCE manacommunity.sports_event_format_id_seq
            OWNED BY manacommunity.sports_event_format.id;
        seq_name := 'manacommunity.sports_event_format_id_seq';
    END IF;

    -- ── 3. Backfill any NULL ids using the sequence ───────────────────
    EXECUTE format(
        'UPDATE manacommunity.sports_event_format SET id = nextval(%L) WHERE id IS NULL',
        seq_name
    );

    -- ── 4. Enforce NOT NULL ───────────────────────────────────────────
    ALTER TABLE manacommunity.sports_event_format ALTER COLUMN id SET NOT NULL;

    -- ── 5. Add PRIMARY KEY — check is INDEPENDENT of column check ─────
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE  conrelid = 'manacommunity.sports_event_format'::regclass
          AND  contype  = 'p'
    ) THEN
        ALTER TABLE manacommunity.sports_event_format ADD PRIMARY KEY (id);
    END IF;

    -- ── 6. Unique (event_id, format) ─────────────────────────────────
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_event_format'
    ) THEN
        ALTER TABLE manacommunity.sports_event_format
            ADD CONSTRAINT uq_event_format UNIQUE (event_id, format);
    END IF;

    -- ── 7. format_id column on registrations ─────────────────────────
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE  table_schema = 'manacommunity'
          AND  table_name   = 'sports_event_registration'
          AND  column_name  = 'format_id'
    ) THEN
        ALTER TABLE manacommunity.sports_event_registration ADD COLUMN format_id BIGINT;
    END IF;

    -- ── 8. FK — same block as PK so constraint is already visible ─────
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_registration_format'
    ) THEN
        ALTER TABLE manacommunity.sports_event_registration
            ADD CONSTRAINT fk_registration_format
            FOREIGN KEY (format_id)
            REFERENCES manacommunity.sports_event_format(id)
            ON DELETE SET NULL;
    END IF;

END $$;

-- Index (IF NOT EXISTS is safe outside a DO block)
CREATE INDEX IF NOT EXISTS idx_reg_format_id
    ON manacommunity.sports_event_registration(format_id);

-- Backfill format_id from existing match_type values
UPDATE manacommunity.sports_event_registration r
SET    format_id = f.id
FROM   manacommunity.sports_event_format f
WHERE  r.format_id IS NULL
  AND  r.event_id  = f.event_id
  AND  r.match_type IS NOT NULL
  AND  UPPER(TRIM(r.match_type)) = UPPER(TRIM(f.format::TEXT));


