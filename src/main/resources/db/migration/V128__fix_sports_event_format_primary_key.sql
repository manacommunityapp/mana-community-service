-- =====================================================================
-- V128: Idempotent safety net for sports_event_format PK + format_id FK.
--       Skips all column/sequence/PK work if PK already exists (V127 ran).
--       Handles both BIGSERIAL and GENERATED ALWAYS AS IDENTITY columns.
-- =====================================================================

DO $$
DECLARE
    seq_name    TEXT;
    is_identity BOOLEAN;
    col_count   INT;
BEGIN

    -- ══ SECTION A: id column + PRIMARY KEY ════════════════════════════
    -- Only enter this section if there is no PK yet on sports_event_format.
    -- When V127 already succeeded, this entire section is skipped.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE  conrelid = 'manacommunity.sports_event_format'::regclass
          AND  contype  = 'p'
    ) THEN

        -- A1. Add id column if missing
        SELECT COUNT(*) INTO col_count
        FROM   information_schema.columns
        WHERE  table_schema = 'manacommunity'
          AND  table_name   = 'sports_event_format'
          AND  column_name  = 'id';

        IF col_count = 0 THEN
            ALTER TABLE manacommunity.sports_event_format ADD COLUMN id BIGINT;
        END IF;

        -- A2. Detect whether the column is an identity column
        SELECT COALESCE((
            SELECT identity_generation IS NOT NULL
            FROM   information_schema.columns
            WHERE  table_schema = 'manacommunity'
              AND  table_name   = 'sports_event_format'
              AND  column_name  = 'id'
        ), FALSE) INTO is_identity;

        IF NOT is_identity THEN
            -- A3a. Regular column: attach sequence if not already attached
            seq_name := pg_get_serial_sequence('manacommunity.sports_event_format', 'id');
            IF seq_name IS NULL THEN
                CREATE SEQUENCE IF NOT EXISTS manacommunity.sports_event_format_id_seq;
                ALTER TABLE manacommunity.sports_event_format
                    ALTER COLUMN id SET DEFAULT nextval('manacommunity.sports_event_format_id_seq');
                ALTER SEQUENCE manacommunity.sports_event_format_id_seq
                    OWNED BY manacommunity.sports_event_format.id;
                seq_name := 'manacommunity.sports_event_format_id_seq';
            END IF;
            -- Backfill NULLs
            EXECUTE format(
                'UPDATE manacommunity.sports_event_format SET id = nextval(%L) WHERE id IS NULL',
                seq_name
            );
        ELSE
            -- A3b. Identity column: find its internal sequence for backfilling NULLs
            SELECT quote_ident(n.nspname) || '.' || quote_ident(s.relname) INTO seq_name
            FROM   pg_class     s
            JOIN   pg_namespace n ON n.oid = s.relnamespace
            JOIN   pg_depend    d ON d.objid = s.oid AND d.deptype = 'i'
            JOIN   pg_class     t ON t.oid = d.refobjid
            WHERE  t.relname        = 'sports_event_format'
              AND  t.relnamespace   = 'manacommunity'::regnamespace
              AND  s.relkind        = 'S'
            LIMIT 1;

            IF seq_name IS NOT NULL THEN
                EXECUTE format(
                    'UPDATE manacommunity.sports_event_format SET id = nextval(%L) WHERE id IS NULL',
                    seq_name
                );
            END IF;
        END IF;

        -- A4. Enforce NOT NULL
        ALTER TABLE manacommunity.sports_event_format ALTER COLUMN id SET NOT NULL;

        -- A5. Add PRIMARY KEY
        ALTER TABLE manacommunity.sports_event_format ADD PRIMARY KEY (id);

    END IF; -- end SECTION A

    -- ══ SECTION B: Remaining constraints (always idempotent) ══════════

    -- B1. Unique (event_id, format)
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_event_format'
    ) THEN
        ALTER TABLE manacommunity.sports_event_format
            ADD CONSTRAINT uq_event_format UNIQUE (event_id, format);
    END IF;

    -- B2. format_id column on registrations
    SELECT COUNT(*) INTO col_count
    FROM   information_schema.columns
    WHERE  table_schema = 'manacommunity'
      AND  table_name   = 'sports_event_registration'
      AND  column_name  = 'format_id';

    IF col_count = 0 THEN
        ALTER TABLE manacommunity.sports_event_registration ADD COLUMN format_id BIGINT;
    END IF;

    -- B3. FK (PK is guaranteed to exist before this runs)
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

-- Index
CREATE INDEX IF NOT EXISTS idx_reg_format_id
    ON manacommunity.sports_event_registration(format_id);

-- Backfill format_id from match_type
UPDATE manacommunity.sports_event_registration r
SET    format_id = f.id
FROM   manacommunity.sports_event_format f
WHERE  r.format_id IS NULL
  AND  r.event_id  = f.event_id
  AND  r.match_type IS NOT NULL
  AND  UPPER(TRIM(r.match_type)) = UPPER(TRIM(f.format::TEXT));