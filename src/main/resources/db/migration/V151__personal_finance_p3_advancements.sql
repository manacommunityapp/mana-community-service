-- V142: Personal Finance P3 Advancements
-- Adds Installments / Loan Amortization and Savings Goals Target tables.

CREATE TABLE IF NOT EXISTS personal_finance_installments (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    monthly_emi NUMERIC(15, 2) NOT NULL,
    interest_rate NUMERIC(5, 2) DEFAULT 0.0,
    total_tenor_months INT NOT NULL,
    remaining_tenor_months INT NOT NULL,
    start_date DATE NOT NULL,
    next_due_date DATE,
    account_id VARCHAR(36),
    category_id VARCHAR(36),
    is_auto_deduct BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pf_inst_user ON personal_finance_installments(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_inst_due ON personal_finance_installments(next_due_date);

CREATE TABLE IF NOT EXISTS personal_finance_goals (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_amount NUMERIC(15, 2) NOT NULL,
    current_amount NUMERIC(15, 2) DEFAULT 0.0,
    target_date DATE,
    icon VARCHAR(100) DEFAULT 'flag',
    color VARCHAR(20) DEFAULT '#10B981',
    category_id VARCHAR(36),
    notes TEXT,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pf_goals_user ON personal_finance_goals(user_id);
