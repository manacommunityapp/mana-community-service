-- V140: Personal Finance Module (My Money Manager)
-- Provides resident-level personal financial management, accounts, transactions,
-- budgets, recurring items, bills, and auto-projections.

CREATE TABLE IF NOT EXISTS personal_finance_accounts (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    credit_limit NUMERIC(15, 2),
    currency VARCHAR(10) NOT NULL DEFAULT '₹',
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    color VARCHAR(50) NOT NULL DEFAULT '#3B82F6',
    icon VARCHAR(100) NOT NULL DEFAULT 'wallet-outline',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_accounts_user_id ON personal_finance_accounts(user_id);

CREATE TABLE IF NOT EXISTS personal_finance_categories (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(100) NOT NULL DEFAULT 'ellipse-outline',
    color VARCHAR(50) NOT NULL DEFAULT '#6B7280',
    type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE
    parent_id VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_categories_user_id ON personal_finance_categories(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_categories_parent_id ON personal_finance_categories(parent_id);

CREATE TABLE IF NOT EXISTS personal_finance_transactions (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE, TRANSFER
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT '₹',
    category_id VARCHAR(64),
    category_name VARCHAR(100),
    category_icon VARCHAR(100),
    category_color VARCHAR(50),
    subcategory_name VARCHAR(100),
    account_id VARCHAR(64) NOT NULL,
    account_name VARCHAR(100),
    to_account_id VARCHAR(64),
    to_account_name VARCHAR(100),
    description VARCHAR(255) NOT NULL,
    notes TEXT,
    transaction_date DATE NOT NULL,
    is_mana_projection BOOLEAN NOT NULL DEFAULT FALSE,
    source_module VARCHAR(50),
    source_type VARCHAR(50),
    source_id VARCHAR(100),
    source_label VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_transactions_user_id ON personal_finance_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_transactions_date ON personal_finance_transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_pf_transactions_account_id ON personal_finance_transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_pf_transactions_category_id ON personal_finance_transactions(category_id);

CREATE TABLE IF NOT EXISTS personal_finance_budgets (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    category_id VARCHAR(64) NOT NULL,
    period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    limit_amount NUMERIC(15, 2) NOT NULL,
    alert_threshold INT NOT NULL DEFAULT 80,
    month VARCHAR(10) NOT NULL, -- YYYY-MM
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_budgets_user_month ON personal_finance_budgets(user_id, month);

CREATE TABLE IF NOT EXISTS personal_finance_bills (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    due_date DATE NOT NULL,
    category_id VARCHAR(64),
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_days_before INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_bills_user_id ON personal_finance_bills(user_id);

CREATE TABLE IF NOT EXISTS personal_finance_recurring (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    category_id VARCHAR(64),
    account_id VARCHAR(64) NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    next_due_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_recurring_user_id ON personal_finance_recurring(user_id);
