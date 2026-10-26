-- Phase 6: Link commerce orders to finance invoices auto-generated on completion.

ALTER TABLE commerce_order
    ADD COLUMN IF NOT EXISTS invoice_document_id BIGINT
        REFERENCES finance_documents(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_co_invoice_document
    ON commerce_order(invoice_document_id)
    WHERE invoice_document_id IS NOT NULL;
