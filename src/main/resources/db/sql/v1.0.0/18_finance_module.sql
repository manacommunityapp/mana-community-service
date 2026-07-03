-- FILE: db/sql/v1.0.0/18_finance_module.sql
-- Income & Expense (Ledger) finance module: customers, unified financial
-- documents (invoice / estimate / sales order / credit note / refund) with
-- line items, and receipts (invoice / advance / other income).
-- Required for prod (ddl-auto: validate); local=create / dev=update auto-create
-- these. Idempotent.

CREATE TABLE IF NOT EXISTS ledger_customers (
    id              BIGSERIAL     PRIMARY KEY,
    name            VARCHAR(160)  NOT NULL,
    email           VARCHAR(160),
    phone           VARCHAR(40),
    gst_type        VARCHAR(40),
    gstin           VARCHAR(20),
    currency        VARCHAR(8),
    opening_balance NUMERIC(14,2),
    balance_type    VARCHAR(4),
    starts_from     DATE,
    bill_addr1      VARCHAR(200),
    bill_addr2      VARCHAR(200),
    bill_city       VARCHAR(80),
    bill_state      VARCHAR(80),
    bill_country    VARCHAR(80),
    bill_zipcode    VARCHAR(20),
    ship_addr1      VARCHAR(200),
    ship_addr2      VARCHAR(200),
    ship_city       VARCHAR(80),
    ship_state      VARCHAR(80),
    ship_country    VARCHAR(80),
    ship_zipcode    VARCHAR(20),
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS finance_documents (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
    type          VARCHAR(16)   NOT NULL,   -- INVOICE | ESTIMATE | SALES_ORDER | CREDIT_NOTE | REFUND
    status        VARCHAR(30),
    customer_id   BIGINT,
    customer_name VARCHAR(160),
    doc_date      DATE          NOT NULL,
    due_date      DATE,
    notes         TEXT,
    terms         TEXT,
    tax_inclusive BOOLEAN       NOT NULL DEFAULT FALSE,
    currency      VARCHAR(8),
    subtotal      NUMERIC(14,2),
    discount      NUMERIC(14,2),
    tax           NUMERIC(14,2),
    other_charges NUMERIC(14,2),
    grand_total   NUMERIC(14,2),
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS finance_document_lines (
    id          BIGSERIAL     PRIMARY KEY,
    document_id BIGINT        NOT NULL REFERENCES finance_documents(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description VARCHAR(400),
    qty         INTEGER       NOT NULL,
    cost        NUMERIC(14,2),
    disc        NUMERIC(6,2),
    tax         NUMERIC(6,2),
    line_total  NUMERIC(14,2)
);

CREATE TABLE IF NOT EXISTS finance_receipts (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
    receipt_type  VARCHAR(12)   NOT NULL,   -- INVOICE | ADVANCE | OTHER
    customer_id   BIGINT,
    customer_name VARCHAR(160),
    receipt_date  DATE          NOT NULL,
    amount        NUMERIC(14,2),
    payment_mode  VARCHAR(40),
    reference     VARCHAR(120),
    notes         TEXT,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_finance_documents_type ON finance_documents (type);
CREATE INDEX IF NOT EXISTS idx_finance_document_lines_doc ON finance_document_lines (document_id);
CREATE INDEX IF NOT EXISTS idx_finance_receipts_type ON finance_receipts (receipt_type);
