-- V108: Hibernate Envers — _aud shadow tables for all Tier 1 entities.
-- Rules:
--   • PK is always (id, rev) — a composite of the entity PK and the revision number.
--   • revtype: 0=INSERT, 1=UPDATE, 2=DELETE. DELETE rows carry NULLs for all columns.
--   • No NOT NULL constraints on audited columns (DELETE rows only carry id/rev/revtype).
--   • FK to revinfo(rev) ensures referential integrity per revision.
--   • No FK back to the live table — Envers manages that relationship in code.

-- ─── CFBOS Accounting ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS manacommunity.cfbos_fiscal_year_aud (
    id          BIGINT    NOT NULL,
    rev         INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype     SMALLINT  NOT NULL,
    name        VARCHAR(20),
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20),
    is_current  BOOLEAN,
    closed_by   BIGINT,
    closed_at   TIMESTAMP,
    created_by  BIGINT,
    created_at  TIMESTAMP,
    updated_by  BIGINT,
    updated_at  TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.cfbos_accounting_period_aud (
    id              BIGINT    NOT NULL,
    rev             INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype         SMALLINT  NOT NULL,
    fiscal_year_id  BIGINT,
    name            VARCHAR(50),
    start_date      DATE,
    end_date        DATE,
    period_number   INT,
    status          VARCHAR(20),
    closed_by       BIGINT,
    closed_at       TIMESTAMP,
    created_at      TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.cfbos_account_aud (
    id                BIGINT    NOT NULL,
    rev               INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype           SMALLINT  NOT NULL,
    code              VARCHAR(20),
    name              VARCHAR(150),
    account_group_id  BIGINT,
    parent_account_id BIGINT,
    account_type      VARCHAR(20),
    is_system_account BOOLEAN,
    is_bank_account   BOOLEAN,
    is_active         BOOLEAN,
    opening_balance   NUMERIC(18,2),
    current_balance   NUMERIC(18,2),
    description       VARCHAR(255),
    created_by        BIGINT,
    created_at        TIMESTAMP,
    updated_by        BIGINT,
    updated_at        TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.cfbos_journal_entry_aud (
    id                    BIGINT    NOT NULL,
    rev                   INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype               SMALLINT  NOT NULL,
    entry_number          VARCHAR(30),
    entry_date            DATE,
    fiscal_year_id        BIGINT,
    accounting_period_id  BIGINT,
    entry_type            VARCHAR(20),
    source_module         VARCHAR(30),
    source_document_type  VARCHAR(50),
    source_document_id    BIGINT,
    narration             TEXT,
    total_debit           NUMERIC(18,2),
    total_credit          NUMERIC(18,2),
    status                VARCHAR(20),
    posted_by             BIGINT,
    posted_at             TIMESTAMP,
    reversed_by           BIGINT,
    reversed_at           TIMESTAMP,
    reversal_of_id        BIGINT,
    version               INT,
    created_by            BIGINT,
    created_at            TIMESTAMP,
    updated_by            BIGINT,
    updated_at            TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.cfbos_journal_line_aud (
    id                BIGINT    NOT NULL,
    rev               INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype           SMALLINT  NOT NULL,
    journal_entry_id  BIGINT,
    account_id        BIGINT,
    cost_center_id    BIGINT,
    fund_id           BIGINT,
    debit_amount      NUMERIC(18,2),
    credit_amount     NUMERIC(18,2),
    narration         VARCHAR(255),
    PRIMARY KEY (id, rev)
);

-- ─── Finance ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS manacommunity.finance_invoice_aud (
    id             BIGINT    NOT NULL,
    rev            INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype        SMALLINT  NOT NULL,
    code           VARCHAR(24),
    status         VARCHAR(30),
    customer_id    BIGINT,
    customer_name  VARCHAR(160),
    doc_date       DATE,
    due_date       DATE,
    notes          TEXT,
    terms          TEXT,
    tax_inclusive  BOOLEAN,
    currency       VARCHAR(8),
    subtotal       NUMERIC(14,2),
    discount       NUMERIC(14,2),
    tax            NUMERIC(14,2),
    other_charges  NUMERIC(14,2),
    grand_total    NUMERIC(14,2),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.finance_receipts_aud (
    id            BIGINT    NOT NULL,
    rev           INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype       SMALLINT  NOT NULL,
    code          VARCHAR(24),
    receipt_type  VARCHAR(12),
    customer_id   BIGINT,
    customer_name VARCHAR(160),
    receipt_date  DATE,
    amount        NUMERIC(14,2),
    payment_mode  VARCHAR(40),
    reference     VARCHAR(120),
    notes         TEXT,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.finance_purchase_aud (
    id             BIGINT    NOT NULL,
    rev            INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype        SMALLINT  NOT NULL,
    code           VARCHAR(24),
    status         VARCHAR(30),
    vendor_id      BIGINT,
    vendor_name    VARCHAR(160),
    doc_date       DATE,
    due_date       DATE,
    notes          TEXT,
    terms          TEXT,
    tax_inclusive  BOOLEAN,
    currency       VARCHAR(8),
    subtotal       NUMERIC(14,2),
    discount       NUMERIC(14,2),
    tax            NUMERIC(14,2),
    other_charges  NUMERIC(14,2),
    grand_total    NUMERIC(14,2),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id, rev)
);

-- ─── User & Roles ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS manacommunity.app_user_aud (
    id                   BIGINT    NOT NULL,
    rev                  INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype              SMALLINT  NOT NULL,
    full_name            VARCHAR(100),
    email                VARCHAR(120),
    phone                VARCHAR(15),
    date_of_birth        DATE,
    gender               VARCHAR(10),
    profile_pic_url      TEXT,
    role                 VARCHAR(255),
    role_id              BIGINT,
    role_changed_at      TIMESTAMP,
    role_changed_by      BIGINT,
    kyc_status           VARCHAR(20),
    govt_id_type         VARCHAR(20),
    govt_id_number       VARCHAR(255),
    flat_no              VARCHAR(20),
    block                VARCHAR(20),
    employee_id          VARCHAR(50),
    tower                VARCHAR(30),
    resident_type        VARCHAR(30),
    occupancy_status     VARCHAR(30),
    community_id         BIGINT,
    is_active            BOOLEAN,
    notify_email         BOOLEAN,
    notify_sms           BOOLEAN,
    notify_whatsapp      BOOLEAN,
    notify_push          BOOLEAN,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.roles_aud (
    id            BIGINT    NOT NULL,
    rev           INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype       SMALLINT  NOT NULL,
    name          VARCHAR(255),
    community_id  BIGINT,
    PRIMARY KEY (id, rev)
);

-- ─── Privacy / Compliance ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS manacommunity.user_privacy_settings_aud (
    id                          BIGINT    NOT NULL,
    rev                         INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype                     SMALLINT  NOT NULL,
    user_id                     BIGINT,
    show_phone_to_neighbours    BOOLEAN,
    show_email_to_neighbours    BOOLEAN,
    show_flat_in_directory      BOOLEAN,
    show_family_members         BOOLEAN,
    show_vehicle_in_directory   BOOLEAN,
    emergency_contact_restricted BOOLEAN,
    allow_marketplace_contact   BOOLEAN,
    allow_event_tagging         BOOLEAN,
    activity_visibility         VARCHAR(30),
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.data_deletion_request_aud (
    id                  BIGINT    NOT NULL,
    rev                 INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype             SMALLINT  NOT NULL,
    user_id             BIGINT,
    community_id        BIGINT,
    status              VARCHAR(30),
    reason              VARCHAR(500),
    verification_token  VARCHAR(100),
    verification_expiry TIMESTAMP,
    requested_at        TIMESTAMP,
    processed_at        TIMESTAMP,
    processed_by        BIGINT,
    notes               TEXT,
    PRIMARY KEY (id, rev)
);

-- ─── VMS (Vendor Management) ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS manacommunity.vms_invoices_aud (
    id               BIGINT    NOT NULL,
    rev              INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype          SMALLINT  NOT NULL,
    invoice_number   VARCHAR(30),
    vendor_id        BIGINT,
    booking_id       BIGINT,
    work_order_id    BIGINT,
    po_id            BIGINT,
    contract_id      BIGINT,
    status           VARCHAR(30),
    invoice_date     DATE,
    due_date         DATE,
    subtotal         NUMERIC(14,2),
    discount_amount  NUMERIC(12,2),
    tax_amount       NUMERIC(12,2),
    tds_amount       NUMERIC(12,2),
    total_amount     NUMERIC(14,2),
    paid_amount      NUMERIC(14,2),
    notes            VARCHAR(1000),
    community_id     BIGINT,
    created_by       BIGINT,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.vms_payments_aud (
    id                BIGINT    NOT NULL,
    rev               INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype           SMALLINT  NOT NULL,
    payment_number    VARCHAR(30),
    vendor_id         BIGINT,
    invoice_id        BIGINT,
    booking_id        BIGINT,
    type              VARCHAR(30),
    status            VARCHAR(30),
    amount            NUMERIC(14,2),
    payment_method    VARCHAR(30),
    transaction_id    VARCHAR(100),
    payment_date      DATE,
    notes             VARCHAR(500),
    gst_amount        NUMERIC(12,2),
    tds_amount        NUMERIC(12,2),
    commission_amount NUMERIC(12,2),
    net_amount        NUMERIC(14,2),
    community_id      BIGINT,
    processed_by      BIGINT,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS manacommunity.vms_contracts_aud (
    id                   BIGINT    NOT NULL,
    rev                  INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype              SMALLINT  NOT NULL,
    contract_number      VARCHAR(30),
    title                VARCHAR(200),
    description          VARCHAR(2000),
    type                 VARCHAR(30),
    status               VARCHAR(30),
    vendor_id            BIGINT,
    start_date           DATE,
    end_date             DATE,
    value                NUMERIC(14,2),
    payment_frequency    VARCHAR(20),
    auto_renew           BOOLEAN,
    renewal_notice_days  INT,
    penalty_clause       VARCHAR(2000),
    termination_clause   VARCHAR(2000),
    signed_by_vendor     BOOLEAN,
    signed_by_admin      BOOLEAN,
    signed_at            TIMESTAMP,
    community_id         BIGINT,
    created_by           BIGINT,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    PRIMARY KEY (id, rev)
);

-- ─── Indexes on rev for cross-entity revision lookups ─────────────────────────

CREATE INDEX IF NOT EXISTS idx_cfbos_fiscal_year_aud_rev          ON manacommunity.cfbos_fiscal_year_aud (rev);
CREATE INDEX IF NOT EXISTS idx_cfbos_accounting_period_aud_rev    ON manacommunity.cfbos_accounting_period_aud (rev);
CREATE INDEX IF NOT EXISTS idx_cfbos_account_aud_rev              ON manacommunity.cfbos_account_aud (rev);
CREATE INDEX IF NOT EXISTS idx_cfbos_journal_entry_aud_rev        ON manacommunity.cfbos_journal_entry_aud (rev);
CREATE INDEX IF NOT EXISTS idx_cfbos_journal_line_aud_rev         ON manacommunity.cfbos_journal_line_aud (rev);
CREATE INDEX IF NOT EXISTS idx_finance_invoice_aud_rev            ON manacommunity.finance_invoice_aud (rev);
CREATE INDEX IF NOT EXISTS idx_finance_receipts_aud_rev           ON manacommunity.finance_receipts_aud (rev);
CREATE INDEX IF NOT EXISTS idx_finance_purchase_aud_rev           ON manacommunity.finance_purchase_aud (rev);
CREATE INDEX IF NOT EXISTS idx_app_user_aud_rev                   ON manacommunity.app_user_aud (rev);
CREATE INDEX IF NOT EXISTS idx_roles_aud_rev                      ON manacommunity.roles_aud (rev);
CREATE INDEX IF NOT EXISTS idx_user_privacy_settings_aud_rev      ON manacommunity.user_privacy_settings_aud (rev);
CREATE INDEX IF NOT EXISTS idx_data_deletion_request_aud_rev      ON manacommunity.data_deletion_request_aud (rev);
CREATE INDEX IF NOT EXISTS idx_vms_invoices_aud_rev               ON manacommunity.vms_invoices_aud (rev);
CREATE INDEX IF NOT EXISTS idx_vms_payments_aud_rev               ON manacommunity.vms_payments_aud (rev);
CREATE INDEX IF NOT EXISTS idx_vms_contracts_aud_rev              ON manacommunity.vms_contracts_aud (rev);
