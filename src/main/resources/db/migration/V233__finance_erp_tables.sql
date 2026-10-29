-- Finance ERP module: BRS, cheque tracker, TDS challan, PG settlement

CREATE TABLE brs_session (
    id                  BIGSERIAL    PRIMARY KEY,
    community_id        BIGINT       NOT NULL,
    bank_account_name   VARCHAR(120) NOT NULL,
    statement_month     VARCHAR(10)  NOT NULL,
    statement_from_date DATE,
    statement_to_date   DATE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    uploaded_at         TIMESTAMP,
    locked_at           TIMESTAMP,
    opening_balance     NUMERIC(16,2) NOT NULL DEFAULT 0,
    closing_balance     NUMERIC(16,2) NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_brs_session_community ON brs_session(community_id);

CREATE TABLE brs_line (
    id                  BIGSERIAL    PRIMARY KEY,
    session_id          BIGINT       NOT NULL REFERENCES brs_session(id) ON DELETE CASCADE,
    value_date          DATE         NOT NULL,
    description         VARCHAR(500) NOT NULL,
    credit_amount       NUMERIC(16,2),
    debit_amount        NUMERIC(16,2),
    utr_reference       VARCHAR(100),
    match_status        VARCHAR(30)  NOT NULL DEFAULT 'UNMATCHED',
    matched_entity_id   BIGINT,
    matched_entity_type VARCHAR(40),
    manual_note         VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_brs_line_session      ON brs_line(session_id);
CREATE INDEX idx_brs_line_match_status ON brs_line(match_status);

CREATE TABLE cheque_payment_record (
    id               BIGSERIAL    PRIMARY KEY,
    community_id     BIGINT       NOT NULL,
    resident_user_id BIGINT,
    flat_no          VARCHAR(30),
    amount           NUMERIC(14,2) NOT NULL,
    cheque_number    VARCHAR(30)  NOT NULL,
    bank_name        VARCHAR(100),
    cheque_date      DATE,
    deposited_date   DATE,
    cleared_date     DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'IN_HAND',
    invoice_id       BIGINT,
    remarks          TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cheque_community ON cheque_payment_record(community_id);
CREATE INDEX idx_cheque_status     ON cheque_payment_record(status);

CREATE TABLE tds_challan (
    id               BIGSERIAL    PRIMARY KEY,
    community_id     BIGINT       NOT NULL,
    financial_year   VARCHAR(10)  NOT NULL,
    quarter          VARCHAR(10)  NOT NULL,
    tds_section      VARCHAR(20)  NOT NULL,
    total_tds_amount NUMERIC(14,2) NOT NULL,
    challan_number   VARCHAR(40),
    bank_name        VARCHAR(100),
    deposited_date   DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    remarks          TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tds_challan_community       ON tds_challan(community_id);
CREATE INDEX idx_tds_challan_year_quarter    ON tds_challan(financial_year, quarter);

CREATE TABLE pg_settlement (
    id                BIGSERIAL    PRIMARY KEY,
    community_id      BIGINT       NOT NULL,
    settlement_date   DATE         NOT NULL,
    utr_number        VARCHAR(60),
    gross_amount      NUMERIC(16,2) NOT NULL,
    mdr_amount        NUMERIC(14,2) NOT NULL DEFAULT 0,
    gst_on_mdr        NUMERIC(14,2) NOT NULL DEFAULT 0,
    net_amount        NUMERIC(16,2) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    transaction_count INT          NOT NULL DEFAULT 0,
    bank_account      VARCHAR(120),
    remarks           TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pg_settlement_community ON pg_settlement(community_id);
CREATE INDEX idx_pg_settlement_date      ON pg_settlement(settlement_date);

-- Extend community_settings with accounting freeze controls
ALTER TABLE community_settings
    ADD COLUMN IF NOT EXISTS accounting_freeze_date DATE NULL,
    ADD COLUMN IF NOT EXISTS accounting_freeze_note TEXT NULL;
