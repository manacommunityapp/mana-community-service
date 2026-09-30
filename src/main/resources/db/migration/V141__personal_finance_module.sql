-- Personal Finance module: accounts, transactions, categories, budgets, bills, recurring transactions

-- Accounts (bank, cash, wallet, credit card, etc.)
CREATE TABLE IF NOT EXISTS manacommunity.pf_account (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL,
    account_name          VARCHAR(100) NOT NULL,
    account_type          VARCHAR(30) NOT NULL DEFAULT 'BANK',
    balance               NUMERIC(15,2) NOT NULL DEFAULT 0,
    currency              VARCHAR(3) NOT NULL DEFAULT 'INR',
    institution           VARCHAR(50),
    account_number_masked VARCHAR(30),
    color                 VARCHAR(20),
    icon                  VARCHAR(50),
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    include_in_total      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_account_user FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_pf_account_user ON manacommunity.pf_account(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_account_active ON manacommunity.pf_account(user_id, is_active);

-- Categories (income/expense, user-defined + system defaults)
CREATE TABLE IF NOT EXISTS manacommunity.pf_category (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT,
    name          VARCHAR(80) NOT NULL,
    category_type VARCHAR(20) NOT NULL,
    icon          VARCHAR(50),
    color         VARCHAR(20),
    is_system     BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_category_user FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_pf_category_user ON manacommunity.pf_category(user_id);

-- Seed system categories
INSERT INTO manacommunity.pf_category (name, category_type, icon, is_system, sort_order) VALUES
    ('Salary',           'INCOME',  'briefcase',    TRUE, 1),
    ('Freelance',        'INCOME',  'laptop',       TRUE, 2),
    ('Investment',       'INCOME',  'trending-up',  TRUE, 3),
    ('Other Income',     'INCOME',  'plus-circle',  TRUE, 4),
    ('Maintenance',      'EXPENSE', 'home',         TRUE, 1),
    ('Groceries',        'EXPENSE', 'shopping-cart', TRUE, 2),
    ('Utilities',        'EXPENSE', 'zap',          TRUE, 3),
    ('Transport',        'EXPENSE', 'truck',        TRUE, 4),
    ('Dining',           'EXPENSE', 'coffee',       TRUE, 5),
    ('Shopping',         'EXPENSE', 'shopping-bag', TRUE, 6),
    ('Healthcare',       'EXPENSE', 'heart',        TRUE, 7),
    ('Education',        'EXPENSE', 'book',         TRUE, 8),
    ('Entertainment',    'EXPENSE', 'film',         TRUE, 9),
    ('Insurance',        'EXPENSE', 'shield',       TRUE, 10),
    ('EMI / Loan',       'EXPENSE', 'credit-card',  TRUE, 11),
    ('Other Expense',    'EXPENSE', 'minus-circle', TRUE, 12);

-- Transactions
CREATE TABLE IF NOT EXISTS manacommunity.pf_transaction (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    account_id              BIGINT NOT NULL,
    txn_type                VARCHAR(20) NOT NULL,
    amount                  NUMERIC(15,2) NOT NULL,
    category_id             BIGINT,
    txn_date                DATE NOT NULL,
    description             VARCHAR(255),
    payee                   VARCHAR(255),
    transfer_to_account_id  BIGINT,
    source_module           VARCHAR(50),
    source_ref_id           BIGINT,
    notes                   TEXT,
    is_recurring_instance   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_txn_user     FOREIGN KEY (user_id)    REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_pf_txn_account  FOREIGN KEY (account_id) REFERENCES manacommunity.pf_account(id),
    CONSTRAINT fk_pf_txn_category FOREIGN KEY (category_id) REFERENCES manacommunity.pf_category(id),
    CONSTRAINT fk_pf_txn_transfer FOREIGN KEY (transfer_to_account_id) REFERENCES manacommunity.pf_account(id)
);

CREATE INDEX IF NOT EXISTS idx_pf_txn_user     ON manacommunity.pf_transaction(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_txn_account  ON manacommunity.pf_transaction(user_id, account_id);
CREATE INDEX IF NOT EXISTS idx_pf_txn_date     ON manacommunity.pf_transaction(user_id, txn_date);
CREATE INDEX IF NOT EXISTS idx_pf_txn_type     ON manacommunity.pf_transaction(user_id, txn_type, txn_date);
CREATE INDEX IF NOT EXISTS idx_pf_txn_category ON manacommunity.pf_transaction(user_id, category_id, txn_date);

-- Budgets
CREATE TABLE IF NOT EXISTS manacommunity.pf_budget (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    category_id         BIGINT NOT NULL,
    budget_amount       NUMERIC(15,2) NOT NULL,
    period              VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    budget_year         INT NOT NULL,
    budget_month        INT,
    alert_threshold_pct INT NOT NULL DEFAULT 80,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_budget_user     FOREIGN KEY (user_id)     REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_pf_budget_category FOREIGN KEY (category_id) REFERENCES manacommunity.pf_category(id),
    CONSTRAINT uq_pf_budget_unique   UNIQUE (user_id, category_id, budget_year, budget_month)
);

CREATE INDEX IF NOT EXISTS idx_pf_budget_user ON manacommunity.pf_budget(user_id, budget_year);

-- Bills
CREATE TABLE IF NOT EXISTS manacommunity.pf_bill (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    name              VARCHAR(150) NOT NULL,
    amount            NUMERIC(15,2) NOT NULL,
    due_date          DATE NOT NULL,
    category_id       BIGINT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_recurring      BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_period VARCHAR(20),
    auto_pay          BOOLEAN NOT NULL DEFAULT FALSE,
    notes             TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_bill_user     FOREIGN KEY (user_id)     REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_pf_bill_category FOREIGN KEY (category_id) REFERENCES manacommunity.pf_category(id)
);

CREATE INDEX IF NOT EXISTS idx_pf_bill_user   ON manacommunity.pf_bill(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_bill_status ON manacommunity.pf_bill(user_id, status);
CREATE INDEX IF NOT EXISTS idx_pf_bill_due    ON manacommunity.pf_bill(user_id, due_date);

-- Recurring Transactions
CREATE TABLE IF NOT EXISTS manacommunity.pf_recurring_txn (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    account_id    BIGINT NOT NULL,
    txn_type      VARCHAR(20) NOT NULL,
    amount        NUMERIC(15,2) NOT NULL,
    category_id   BIGINT,
    description   VARCHAR(255),
    payee         VARCHAR(255),
    frequency     VARCHAR(20) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE,
    next_due_date DATE NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_recurring_user     FOREIGN KEY (user_id)     REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_pf_recurring_account  FOREIGN KEY (account_id)  REFERENCES manacommunity.pf_account(id),
    CONSTRAINT fk_pf_recurring_category FOREIGN KEY (category_id) REFERENCES manacommunity.pf_category(id)
);

CREATE INDEX IF NOT EXISTS idx_pf_recurring_user ON manacommunity.pf_recurring_txn(user_id);
CREATE INDEX IF NOT EXISTS idx_pf_recurring_due  ON manacommunity.pf_recurring_txn(is_active, next_due_date);
