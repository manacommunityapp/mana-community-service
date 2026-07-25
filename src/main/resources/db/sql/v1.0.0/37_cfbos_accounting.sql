-- src/main/resources/db/sql/v1.0.0/37_cfbos_accounting.sql

CREATE TABLE IF NOT EXISTS cfbos_fiscal_year (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    closed_by BIGINT,
    closed_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_accounting_period (
    id BIGSERIAL PRIMARY KEY,
    fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    period_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_by BIGINT,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_account_group (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    parent_group_id BIGINT REFERENCES cfbos_account_group(id),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_account (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    account_group_id BIGINT NOT NULL REFERENCES cfbos_account_group(id),
    parent_account_id BIGINT REFERENCES cfbos_account(id),
    account_type VARCHAR(20) NOT NULL,
    is_system_account BOOLEAN NOT NULL DEFAULT FALSE,
    is_bank_account BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    opening_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    description TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_journal_entry (
    id BIGSERIAL PRIMARY KEY,
    entry_number VARCHAR(30) NOT NULL UNIQUE,
    entry_date DATE NOT NULL,
    fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
    accounting_period_id BIGINT NOT NULL REFERENCES cfbos_accounting_period(id),
    entry_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    source_module VARCHAR(30),
    source_document_type VARCHAR(50),
    source_document_id BIGINT,
    narration TEXT NOT NULL,
    total_debit NUMERIC(18,2) NOT NULL,
    total_credit NUMERIC(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    posted_by BIGINT,
    posted_at TIMESTAMP,
    reversed_by BIGINT,
    reversed_at TIMESTAMP,
    reversal_of_id BIGINT,
    version INTEGER NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_journal_line (
    id BIGSERIAL PRIMARY KEY,
    journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
    cost_center_id BIGINT,
    fund_id BIGINT,
    debit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    narration VARCHAR(255),
    CONSTRAINT chk_cfbos_journal_line_single_side CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0) OR
        (debit_amount = 0 AND credit_amount = 0)
    )
);

CREATE TABLE IF NOT EXISTS cfbos_cost_center (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES cfbos_cost_center(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cfbos_journal_entry_date ON cfbos_journal_entry(entry_date, status);
CREATE INDEX IF NOT EXISTS idx_cfbos_journal_entry_source ON cfbos_journal_entry(source_module, source_document_id);
CREATE INDEX IF NOT EXISTS idx_cfbos_journal_line_account ON cfbos_journal_line(account_id);
CREATE INDEX IF NOT EXISTS idx_cfbos_journal_line_entry ON cfbos_journal_line(journal_entry_id);
CREATE INDEX IF NOT EXISTS idx_cfbos_account_parent ON cfbos_account(parent_account_id);
CREATE INDEX IF NOT EXISTS idx_cfbos_accounting_period_fy ON cfbos_accounting_period(fiscal_year_id);
