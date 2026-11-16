-- HRM Payroll, Finance & Operations module

-- Salary Components (org-level catalog)
CREATE TABLE IF NOT EXISTS hr_salary_components (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    name             VARCHAR(100) NOT NULL,
    code             VARCHAR(50) NOT NULL,
    component_type   VARCHAR(20) NOT NULL,
    calculation_type VARCHAR(30) NOT NULL,
    default_value    DOUBLE PRECISION,
    is_taxable       BOOLEAN NOT NULL DEFAULT FALSE,
    is_statutory     BOOLEAN NOT NULL DEFAULT FALSE,
    statutory_code   VARCHAR(20),
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    sequence_order   INTEGER DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, code)
);

-- Salary Structures (per-employee)
CREATE TABLE IF NOT EXISTS hr_salary_structures (
    id                          BIGSERIAL PRIMARY KEY,
    organization_id             BIGINT NOT NULL,
    employee_id                 BIGINT NOT NULL REFERENCES hr_employees(id),
    ctc_annual                  DOUBLE PRECISION NOT NULL,
    basic_monthly               DOUBLE PRECISION NOT NULL,
    hra_monthly                 DOUBLE PRECISION DEFAULT 0,
    da_monthly                  DOUBLE PRECISION DEFAULT 0,
    special_allowance_monthly   DOUBLE PRECISION DEFAULT 0,
    other_allowances_monthly    DOUBLE PRECISION DEFAULT 0,
    pf_employee_monthly         DOUBLE PRECISION DEFAULT 0,
    pf_employer_monthly         DOUBLE PRECISION DEFAULT 0,
    esi_employee_monthly        DOUBLE PRECISION DEFAULT 0,
    esi_employer_monthly        DOUBLE PRECISION DEFAULT 0,
    professional_tax_monthly    DOUBLE PRECISION DEFAULT 0,
    tds_monthly                 DOUBLE PRECISION DEFAULT 0,
    effective_from              DATE NOT NULL,
    effective_to                DATE,
    created_by                  BIGINT NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, effective_from)
);

-- Payroll Runs
CREATE TABLE IF NOT EXISTS hr_payroll_runs (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    payroll_month    INTEGER NOT NULL,
    payroll_year     INTEGER NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_gross      DOUBLE PRECISION DEFAULT 0,
    total_deductions DOUBLE PRECISION DEFAULT 0,
    total_net        DOUBLE PRECISION DEFAULT 0,
    employee_count   INTEGER DEFAULT 0,
    processed_by     BIGINT,
    processed_at     TIMESTAMPTZ,
    approved_by      BIGINT,
    approved_at      TIMESTAMPTZ,
    remarks          VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (organization_id, payroll_month, payroll_year)
);

-- Payslips (per employee per run)
CREATE TABLE IF NOT EXISTS hr_payslips (
    id                   BIGSERIAL PRIMARY KEY,
    organization_id      BIGINT NOT NULL,
    employee_id          BIGINT NOT NULL REFERENCES hr_employees(id),
    payroll_run_id       BIGINT NOT NULL REFERENCES hr_payroll_runs(id),
    working_days         INTEGER,
    paid_days            INTEGER,
    loss_of_pay_days     INTEGER DEFAULT 0,
    basic_salary         DOUBLE PRECISION DEFAULT 0,
    hra                  DOUBLE PRECISION DEFAULT 0,
    da                   DOUBLE PRECISION DEFAULT 0,
    special_allowance    DOUBLE PRECISION DEFAULT 0,
    other_allowances     DOUBLE PRECISION DEFAULT 0,
    gross_salary         DOUBLE PRECISION DEFAULT 0,
    pf_employee          DOUBLE PRECISION DEFAULT 0,
    pf_employer          DOUBLE PRECISION DEFAULT 0,
    esi_employee         DOUBLE PRECISION DEFAULT 0,
    esi_employer         DOUBLE PRECISION DEFAULT 0,
    professional_tax     DOUBLE PRECISION DEFAULT 0,
    tds                  DOUBLE PRECISION DEFAULT 0,
    other_deductions     DOUBLE PRECISION DEFAULT 0,
    total_deductions     DOUBLE PRECISION DEFAULT 0,
    net_salary           DOUBLE PRECISION DEFAULT 0,
    reimbursements_paid  DOUBLE PRECISION DEFAULT 0,
    is_published         BOOLEAN NOT NULL DEFAULT FALSE,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, payroll_run_id)
);
CREATE INDEX idx_hr_payslips_employee ON hr_payslips(employee_id, is_published);

-- Expense Claims
CREATE TABLE IF NOT EXISTS hr_expense_claims (
    id                        BIGSERIAL PRIMARY KEY,
    organization_id           BIGINT NOT NULL,
    employee_id               BIGINT NOT NULL REFERENCES hr_employees(id),
    claim_title               VARCHAR(200) NOT NULL,
    claim_type                VARCHAR(30) NOT NULL,
    total_amount              DOUBLE PRECISION DEFAULT 0,
    approved_amount           DOUBLE PRECISION,
    claim_date                DATE NOT NULL,
    description               TEXT,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approved_by               BIGINT,
    approved_at               TIMESTAMPTZ,
    rejection_reason          VARCHAR(500),
    reimbursed_in_payroll_id  BIGINT,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_hr_expense_claims_employee ON hr_expense_claims(employee_id, status);
CREATE INDEX idx_hr_expense_claims_org ON hr_expense_claims(organization_id, status);

-- Expense Items (line items on a claim)
CREATE TABLE IF NOT EXISTS hr_expense_items (
    id               BIGSERIAL PRIMARY KEY,
    expense_claim_id BIGINT NOT NULL REFERENCES hr_expense_claims(id),
    description      VARCHAR(300) NOT NULL,
    amount           DOUBLE PRECISION NOT NULL,
    expense_date     DATE,
    receipt_url      VARCHAR(500),
    category         VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Travel Requests (pre-approval)
CREATE TABLE IF NOT EXISTS hr_travel_requests (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL REFERENCES hr_employees(id),
    destination       VARCHAR(300) NOT NULL,
    purpose           VARCHAR(500) NOT NULL,
    travel_from       DATE NOT NULL,
    travel_to         DATE NOT NULL,
    mode_of_travel    VARCHAR(50),
    estimated_cost    DOUBLE PRECISION,
    advance_requested DOUBLE PRECISION DEFAULT 0,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by       BIGINT,
    approved_at       TIMESTAMPTZ,
    rejection_reason  VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Assets (org-level inventory)
CREATE TABLE IF NOT EXISTS hr_assets (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    asset_name       VARCHAR(200) NOT NULL,
    asset_type       VARCHAR(50) NOT NULL,
    asset_code       VARCHAR(100),
    serial_number    VARCHAR(200),
    make             VARCHAR(100),
    model            VARCHAR(100),
    purchase_date    DATE,
    purchase_value   DOUBLE PRECISION,
    current_value    DOUBLE PRECISION,
    warranty_expiry  DATE,
    status           VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_hr_assets_org ON hr_assets(organization_id, status);

-- Asset Assignments
CREATE TABLE IF NOT EXISTS hr_asset_assignments (
    id                        BIGSERIAL PRIMARY KEY,
    organization_id           BIGINT NOT NULL,
    asset_id                  BIGINT NOT NULL REFERENCES hr_assets(id),
    employee_id               BIGINT NOT NULL REFERENCES hr_employees(id),
    assigned_date             DATE NOT NULL,
    return_date               DATE,
    condition_at_assignment   VARCHAR(50),
    condition_at_return       VARCHAR(50),
    notes                     VARCHAR(500),
    assigned_by               BIGINT NOT NULL,
    is_active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_hr_asset_assignments_employee ON hr_asset_assignments(employee_id, is_active);

-- Tasks
CREATE TABLE IF NOT EXISTS hr_tasks (
    id                        BIGSERIAL PRIMARY KEY,
    organization_id           BIGINT NOT NULL,
    assigned_to_employee_id   BIGINT NOT NULL REFERENCES hr_employees(id),
    assigned_by_employee_id   BIGINT REFERENCES hr_employees(id),
    title                     VARCHAR(300) NOT NULL,
    description               TEXT,
    priority                  VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status                    VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    due_date                  DATE,
    completed_at              TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_hr_tasks_assignee ON hr_tasks(assigned_to_employee_id, status);

-- Announcements
CREATE TABLE IF NOT EXISTS hr_announcements (
    id                    BIGSERIAL PRIMARY KEY,
    organization_id       BIGINT NOT NULL,
    title                 VARCHAR(300) NOT NULL,
    body                  TEXT NOT NULL,
    target_type           VARCHAR(20) NOT NULL DEFAULT 'ALL',
    target_department_id  BIGINT,
    target_employee_id    BIGINT,
    is_pinned             BOOLEAN NOT NULL DEFAULT FALSE,
    published_at          TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    created_by            BIGINT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_hr_announcements_org ON hr_announcements(organization_id, published_at DESC);

-- Help Desk Tickets
CREATE TABLE IF NOT EXISTS hr_helpdesk_tickets (
    id                        BIGSERIAL PRIMARY KEY,
    organization_id           BIGINT NOT NULL,
    employee_id               BIGINT NOT NULL REFERENCES hr_employees(id),
    ticket_number             VARCHAR(20) NOT NULL,
    category                  VARCHAR(30) NOT NULL,
    subject                   VARCHAR(300) NOT NULL,
    description               TEXT NOT NULL,
    priority                  VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status                    VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to_employee_id   BIGINT REFERENCES hr_employees(id),
    resolution                TEXT,
    resolved_at               TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_hr_helpdesk_org ON hr_helpdesk_tickets(organization_id, status);
CREATE INDEX idx_hr_helpdesk_employee ON hr_helpdesk_tickets(employee_id);
