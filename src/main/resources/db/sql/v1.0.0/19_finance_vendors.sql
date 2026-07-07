-- FILE: db/sql/v1.0.0/19_finance_vendors.sql
-- Income & Expense (Ledger) finance module: vendors (the supplier side — the
-- party paid for purchases, purchase orders and debit notes).
-- Required for prod (ddl-auto: validate); local=create / dev=update auto-create
-- this. Idempotent.

CREATE TABLE IF NOT EXISTS finance_vendors (
    id             BIGSERIAL     PRIMARY KEY,
    name           VARCHAR(160)  NOT NULL,
    contact_person VARCHAR(160),
    email          VARCHAR(160),
    phone          VARCHAR(40),
    gstin          VARCHAR(20),
    pan            VARCHAR(20),
    address        VARCHAR(300),
    city           VARCHAR(80),
    state          VARCHAR(80),
    pincode        VARCHAR(20),
    status         VARCHAR(20),
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);
