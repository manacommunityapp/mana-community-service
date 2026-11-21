-- ───────────────────────────────────────────────────────────────────────────────
-- finance_invoice was created without any tenant column (db/sql/v1.0.0/
-- 21_finance_dedicated_documents.sql), so FinanceInvoiceService listed, read,
-- updated and deleted invoices across every community, and summed receivables
-- platform-wide.
--
-- community_id is left NULLABLE on purpose: the owning community of rows that
-- already exist cannot be derived from anything stored on the row (there is no
-- property/resident link on this table and customer_id is not community-scoped),
-- so there is no safe back-fill. Pre-existing rows stay NULL and are therefore
-- invisible to the now community-scoped queries. They need a one-off data fix
-- by whoever knows which community they belong to.
--
-- payment_recorded_at marks the first genuine transition to "Paid". Without it
-- a Paid -> Pending -> Paid status cycle re-published InvoicePaymentReceivedEvent
-- and double-posted DR Cash / CR AR.
-- ───────────────────────────────────────────────────────────────────────────────

ALTER TABLE finance_invoice ADD COLUMN IF NOT EXISTS community_id BIGINT;
ALTER TABLE finance_invoice ADD COLUMN IF NOT EXISTS payment_recorded_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_finance_invoice_community
    ON finance_invoice (community_id);

CREATE INDEX IF NOT EXISTS idx_finance_invoice_community_status
    ON finance_invoice (community_id, status);

COMMENT ON COLUMN finance_invoice.community_id IS
    'Owning community (tenant). NULL on rows created before V502 — not back-fillable.';
COMMENT ON COLUMN finance_invoice.payment_recorded_at IS
    'Set once, on the first transition to Paid; guards against re-publishing the payment event.';
