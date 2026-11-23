-- ───────────────────────────────────────────────────────────────────────────────
-- General ledger idempotency + reference widening.
--
-- 1. reference_id was declared BIGINT in V159, but GeneralLedgerEntry maps it as a
--    String and the PAYMENT posting passes the invoice CODE ("INV/001"), so every
--    payment posting was destined to fail on a Postgres database. Widen it to
--    VARCHAR(100), which also removes the need for the 30-character truncation in
--    GeneralLedgerEntryService that could make distinct references collide.
--
-- 2. postDoubleEntry blindly wrote two rows per call, so toggling an invoice
--    Paid -> Pending -> Paid produced two DR Cash / CR AR pairs for one payment and
--    overstated cash. A unique index on
--    (community_id, reference_type, reference_id, account_code) makes a repeat pair
--    impossible at the database level, independent of the application-level check.
--
--    Pre-existing duplicate rows would make CREATE UNIQUE INDEX fail and therefore
--    break startup, so the statement is attempted inside an exception handler: if
--    legacy duplicates exist the migration falls back to a plain (non-unique) index
--    and RAISEs a WARNING naming the follow-up. The application-level check in
--    GeneralLedgerEntryService holds either way. No ledger rows are ever deleted
--    here — de-duplicating books is an accounting decision, not a migration.
-- ───────────────────────────────────────────────────────────────────────────────

-- 1. Widen reference_id (idempotent; tolerates the column already being VARCHAR).
DO $$
DECLARE
    v_type text;
    v_len  int;
BEGIN
    SELECT data_type, character_maximum_length
      INTO v_type, v_len
      FROM information_schema.columns
     WHERE table_schema = current_schema()
       AND table_name   = 'general_ledger_entry'
       AND column_name  = 'reference_id';

    IF v_type IS NULL THEN
        RAISE NOTICE 'general_ledger_entry.reference_id not found - skipping widen';
    ELSIF v_type <> 'character varying' OR v_len IS NULL OR v_len < 100 THEN
        EXECUTE 'ALTER TABLE general_ledger_entry '
             || 'ALTER COLUMN reference_id TYPE VARCHAR(100) USING reference_id::VARCHAR';
    END IF;
END $$;

COMMENT ON COLUMN general_ledger_entry.reference_id IS
    'Source document identifier (invoice id or invoice code). VARCHAR so codes like INV/001 fit.';

-- 2. Idempotency index: unique where possible, plain index as a safe fallback.
DO $$
BEGIN
    CREATE UNIQUE INDEX IF NOT EXISTS ux_gle_reference_account
        ON general_ledger_entry (community_id, reference_type, reference_id, account_code)
        WHERE reference_type IS NOT NULL AND reference_id IS NOT NULL;
EXCEPTION
    WHEN unique_violation THEN
        RAISE WARNING 'general_ledger_entry already contains duplicate '
                      '(community_id, reference_type, reference_id, account_code) rows; '
                      'created a NON-UNIQUE index instead. De-duplicate the ledger and then '
                      'create ux_gle_reference_account manually.';
        CREATE INDEX IF NOT EXISTS idx_gle_reference_account
            ON general_ledger_entry (community_id, reference_type, reference_id, account_code);
END $$;
