-- V330: Ensure fitness step_log and finance ERP tables exist
-- Guarantees tables exist on environments where earlier migrations (V233, V240)
-- were skipped due to historical Flyway duplicate version collisions.

-- 1. Fitness Step Tracking
CREATE TABLE IF NOT EXISTS manacommunity.step_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    log_date        DATE          NOT NULL,
    step_count      INTEGER       NOT NULL DEFAULT 0,
    distance_km     DECIMAL(8,3),
    active_minutes  INTEGER,
    source          VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    device_id       BIGINT        REFERENCES manacommunity.wearable_device(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_step_log_user_date UNIQUE (user_id, log_date)
);

CREATE INDEX IF NOT EXISTS idx_step_log_user_date ON manacommunity.step_log (user_id, log_date DESC);

-- 2. Finance ERP Tables
CREATE TABLE IF NOT EXISTS manacommunity.brs_session (
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
CREATE INDEX IF NOT EXISTS idx_brs_session_community ON manacommunity.brs_session(community_id);

CREATE TABLE IF NOT EXISTS manacommunity.brs_line (
    id                  BIGSERIAL    PRIMARY KEY,
    session_id          BIGINT       NOT NULL REFERENCES manacommunity.brs_session(id) ON DELETE CASCADE,
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
CREATE INDEX IF NOT EXISTS idx_brs_line_session      ON manacommunity.brs_line(session_id);
CREATE INDEX IF NOT EXISTS idx_brs_line_match_status ON manacommunity.brs_line(match_status);

CREATE TABLE IF NOT EXISTS manacommunity.cheque_payment_record (
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
CREATE INDEX IF NOT EXISTS idx_cheque_community ON manacommunity.cheque_payment_record(community_id);
CREATE INDEX IF NOT EXISTS idx_cheque_status     ON manacommunity.cheque_payment_record(status);

CREATE TABLE IF NOT EXISTS manacommunity.tds_challan (
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
CREATE INDEX IF NOT EXISTS idx_tds_challan_community       ON manacommunity.tds_challan(community_id);
CREATE INDEX IF NOT EXISTS idx_tds_challan_year_quarter    ON manacommunity.tds_challan(financial_year, quarter);

CREATE TABLE IF NOT EXISTS manacommunity.pg_settlement (
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
CREATE INDEX IF NOT EXISTS idx_pg_settlement_community ON manacommunity.pg_settlement(community_id);
CREATE INDEX IF NOT EXISTS idx_pg_settlement_date      ON manacommunity.pg_settlement(settlement_date);

-- 3. Ensure workout_log audit columns exist
ALTER TABLE manacommunity.workout_log
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

