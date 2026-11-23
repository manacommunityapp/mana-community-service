-- ─────────────────────────────────────────────────────────────────────────────
-- V508: One-time SQL backfill for pre-V502 finance_invoice records where community_id IS NULL.
--
-- V502 added community_id to finance_invoice, but left pre-existing rows as NULL.
-- This migration attributes pre-existing records:
-- 1. Derive community_id from app_user via customer_id where matched.
-- 2. Fall back to default community (1) for any remaining NULL records.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE finance_invoice fi
   SET community_id = COALESCE(
       (SELECT au.community_id FROM manacommunity.app_user au WHERE au.id = fi.customer_id AND au.community_id IS NOT NULL),
       1
   )
 WHERE fi.community_id IS NULL;

-- Keep envers audit table in sync if present
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'finance_invoice_aud') THEN
        UPDATE finance_invoice_aud fia
           SET community_id = COALESCE(
               (SELECT fi.community_id FROM finance_invoice fi WHERE fi.id = fia.id),
               1
           )
         WHERE fia.community_id IS NULL;
    END IF;
END $$;

DO $$
DECLARE
    total_count BIGINT;
    null_count  BIGINT;
BEGIN
    SELECT count(*) INTO total_count FROM finance_invoice;
    SELECT count(*) INTO null_count FROM finance_invoice WHERE community_id IS NULL;
    RAISE NOTICE 'V508: finance_invoice backfill complete. Total=%, Remaining NULL=%', total_count, null_count;
END $$;
