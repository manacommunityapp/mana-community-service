-- FILE: db/sql/v1.0.0/21_finance_dedicated_documents.sql
-- Dedicated per-menu financial document tables. Each Income/Expense menu now has
-- its OWN table + line table (replacing the shared finance_documents table for
-- these types): invoices, estimates, sales orders, credit notes (customer side)
-- and purchases, purchase orders, debit notes (vendor side).
-- Required for prod (ddl-auto: validate); local=create / dev=update auto-create
-- these. Idempotent.
--
-- Line tables map an @ElementCollection of FinanceLineItem, so they carry no own
-- primary key — just the owning FK + the embedded line columns.

-- ── Income side (customer) ──────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS finance_invoice (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
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
CREATE TABLE IF NOT EXISTS finance_invoice_line (
    invoice_id  BIGINT        NOT NULL REFERENCES finance_invoice(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_invoice_line_owner ON finance_invoice_line (invoice_id);

CREATE TABLE IF NOT EXISTS finance_estimate (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
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
CREATE TABLE IF NOT EXISTS finance_estimate_line (
    estimate_id BIGINT        NOT NULL REFERENCES finance_estimate(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_estimate_line_owner ON finance_estimate_line (estimate_id);

CREATE TABLE IF NOT EXISTS finance_sales_order (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
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
CREATE TABLE IF NOT EXISTS finance_sales_order_line (
    sales_order_id BIGINT     NOT NULL REFERENCES finance_sales_order(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_sales_order_line_owner ON finance_sales_order_line (sales_order_id);

CREATE TABLE IF NOT EXISTS finance_credit_note (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
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
CREATE TABLE IF NOT EXISTS finance_credit_note_line (
    credit_note_id BIGINT     NOT NULL REFERENCES finance_credit_note(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_credit_note_line_owner ON finance_credit_note_line (credit_note_id);

-- ── Expense side (vendor) ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS finance_purchase (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
    status        VARCHAR(30),
    vendor_id     BIGINT,
    vendor_name   VARCHAR(160),
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
CREATE TABLE IF NOT EXISTS finance_purchase_line (
    purchase_id BIGINT        NOT NULL REFERENCES finance_purchase(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_purchase_line_owner ON finance_purchase_line (purchase_id);

CREATE TABLE IF NOT EXISTS finance_purchase_order (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
    status        VARCHAR(30),
    vendor_id     BIGINT,
    vendor_name   VARCHAR(160),
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
CREATE TABLE IF NOT EXISTS finance_purchase_order_line (
    purchase_order_id BIGINT  NOT NULL REFERENCES finance_purchase_order(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_purchase_order_line_owner ON finance_purchase_order_line (purchase_order_id);

CREATE TABLE IF NOT EXISTS finance_debit_note (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
    status        VARCHAR(30),
    vendor_id     BIGINT,
    vendor_name   VARCHAR(160),
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
CREATE TABLE IF NOT EXISTS finance_debit_note_line (
    debit_note_id BIGINT      NOT NULL REFERENCES finance_debit_note(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_debit_note_line_owner ON finance_debit_note_line (debit_note_id);

CREATE TABLE IF NOT EXISTS finance_business_expense (
    id            BIGSERIAL     PRIMARY KEY,
    code          VARCHAR(24),
    status        VARCHAR(30),
    vendor_id     BIGINT,
    vendor_name   VARCHAR(160),
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
CREATE TABLE IF NOT EXISTS finance_business_expense_line (
    business_expense_id BIGINT NOT NULL REFERENCES finance_business_expense(id) ON DELETE CASCADE,
    item        VARCHAR(200),
    description TEXT,
    qty         INTEGER,
    cost        NUMERIC(14,2),
    disc        NUMERIC(14,2),
    tax         NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);
CREATE INDEX IF NOT EXISTS idx_finance_business_expense_line_owner ON finance_business_expense_line (business_expense_id);
