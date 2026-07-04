-- FILE: db/sql/v1.0.0/20_finance_vendor_payments.sql
-- Income & Expense (Ledger) finance module — Expenses group additions:
--   * finance_vendor_payments: money paid to a vendor (outflow counterpart of
--     finance_receipts).
--   * Debit Notes reuse finance_documents with type = 'DEBIT_NOTE' (the type
--     column is VARCHAR, so no schema change is needed — noted here for clarity).
-- Required for prod (ddl-auto: validate); local=create / dev=update auto-create
-- this. Idempotent.

CREATE TABLE IF NOT EXISTS finance_vendor_payments (
    id           BIGSERIAL     PRIMARY KEY,
    code         VARCHAR(24),
    payment_type VARCHAR(16)   NOT NULL DEFAULT 'PAID_BILL',  -- PAID_BILL | ADVANCE | OTHER
    vendor_id    BIGINT,
    vendor_name  VARCHAR(160),
    payment_date DATE          NOT NULL,
    amount       NUMERIC(14,2),
    payment_mode VARCHAR(40),
    paid_from    VARCHAR(120),
    reference    VARCHAR(120),
    status       VARCHAR(30),
    notes        TEXT,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);
