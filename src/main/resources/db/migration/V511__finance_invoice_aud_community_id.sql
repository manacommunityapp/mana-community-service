-- =============================================================================
-- V511__finance_invoice_aud_community_id.sql
-- Align Hibernate Envers audit table finance_invoice_aud with FinanceInvoice:
-- Adds community_id and payment_recorded_at (introduced to base table in V502).
-- =============================================================================

ALTER TABLE IF EXISTS finance_invoice_aud
    ADD COLUMN IF NOT EXISTS community_id        BIGINT,
    ADD COLUMN IF NOT EXISTS payment_recorded_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_finance_invoice_aud_community
    ON finance_invoice_aud (community_id);

-- Backfill community_id in finance_invoice_aud from finance_invoice where matched
UPDATE finance_invoice_aud fia
   SET community_id = COALESCE(
       (SELECT fi.community_id FROM finance_invoice fi WHERE fi.id = fia.id),
       1
   )
 WHERE fia.community_id IS NULL;
