-- ============================================================================
-- HRM Module - Phase 1 (P0) Database Schema
-- Mana Community Hub - Human Resource Management
-- Multi-tenant workforce management for community businesses
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- HR Organizations - registered businesses that manage employees
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_organizations (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT,
    organization_name VARCHAR(200) NOT NULL,
    organization_type VARCHAR(50) NOT NULL,
    gst_number      VARCHAR(20),
    registration_number VARCHAR(50),
    contact_email   VARCHAR(150),
    contact_phone   VARCHAR(15),
    address         TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_hr_org_community ON hr_organizations(community_id);

-- ----------------------------------------------------------------------------
-- HR Branches - physical locations of an organization
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_branches (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    branch_name     VARCHAR(150) NOT NULL,
    branch_code     VARCHAR(20),
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(10),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_hr_branch_org ON hr_branches(organization_id);

-- ----------------------------------------------------------------------------
-- HR Departments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_departments (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    department_name VARCHAR(150) NOT NULL,
    department_code VARCHAR(20),
    head_employee_id BIGINT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, department_name)
);

CREATE INDEX idx_hr_dept_org ON hr_departments(organization_id);

-- ----------------------------------------------------------------------------
-- HR Designations
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_designations (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    designation_name VARCHAR(150) NOT NULL,
    level           INTEGER,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hr_desig_org ON hr_designations(organization_id);

-- ----------------------------------------------------------------------------
-- HR Employees - the core employee master table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_employees (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    branch_id       BIGINT REFERENCES hr_branches(id),
    department_id   BIGINT REFERENCES hr_departments(id),
    designation_id  BIGINT REFERENCES hr_designations(id),
    mana_user_id    BIGINT NOT NULL,
    employee_code   VARCHAR(50) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    personal_email  VARCHAR(150),
    work_email      VARCHAR(150),
    phone           VARCHAR(15),
    employment_type VARCHAR(30) NOT NULL,
    employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joining_date    DATE,
    confirmation_date DATE,
    exit_date       DATE,
    reporting_manager_id BIGINT REFERENCES hr_employees(id),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(15),
    version         BIGINT DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    UNIQUE (organization_id, employee_code),
    UNIQUE (organization_id, mana_user_id)
);

CREATE INDEX idx_hr_employee_org ON hr_employees(organization_id, employment_status);
CREATE INDEX idx_hr_employee_mana_user ON hr_employees(mana_user_id);

-- ----------------------------------------------------------------------------
-- HR Attendance - daily attendance summary per employee
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_attendance (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    attendance_date DATE NOT NULL,
    first_check_in  TIME,
    last_check_out  TIME,
    total_work_minutes INTEGER DEFAULT 0,
    attendance_status VARCHAR(30) NOT NULL,
    remarks         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, attendance_date)
);

CREATE INDEX idx_hr_attendance_employee_date ON hr_attendance(employee_id, attendance_date);
CREATE INDEX idx_hr_attendance_org_date ON hr_attendance(organization_id, attendance_date);

-- ----------------------------------------------------------------------------
-- HR Attendance Logs - immutable check-in/check-out event log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_attendance_logs (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    event_type      VARCHAR(20) NOT NULL,
    event_time      TIMESTAMPTZ NOT NULL,
    source          VARCHAR(30),
    latitude        NUMERIC(10,7),
    longitude       NUMERIC(10,7),
    device_id       VARCHAR(150),
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hr_att_log_employee_time ON hr_attendance_logs(employee_id, event_time);

-- ----------------------------------------------------------------------------
-- HR Leave Types - configurable leave policies per organization
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_leave_types (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    leave_type_name VARCHAR(50) NOT NULL,
    default_balance INTEGER NOT NULL,
    is_paid         BOOLEAN NOT NULL DEFAULT TRUE,
    is_carry_forward BOOLEAN NOT NULL DEFAULT FALSE,
    max_carry_forward INTEGER,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hr_leave_type_org ON hr_leave_types(organization_id);

-- ----------------------------------------------------------------------------
-- HR Leave Balances - per employee per leave type per year
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    leave_type_id   BIGINT NOT NULL REFERENCES hr_leave_types(id),
    year            INTEGER NOT NULL,
    total_balance   NUMERIC(5,2) NOT NULL DEFAULT 0,
    used_balance    NUMERIC(5,2) NOT NULL DEFAULT 0,
    remaining_balance NUMERIC(5,2) NOT NULL DEFAULT 0,
    version         BIGINT DEFAULT 0,
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, leave_type_id, year)
);

CREATE INDEX idx_hr_leave_bal_emp_year ON hr_leave_balances(employee_id, year);

-- ----------------------------------------------------------------------------
-- HR Leave Requests
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_leave_requests (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    leave_type_id   BIGINT NOT NULL REFERENCES hr_leave_types(id),
    from_date       DATE NOT NULL,
    to_date         DATE NOT NULL,
    total_days      NUMERIC(5,2) NOT NULL,
    reason          TEXT,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_by     BIGINT,
    approver_remarks TEXT,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_hr_leave_req_org_status ON hr_leave_requests(organization_id, approval_status);
CREATE INDEX idx_hr_leave_req_employee ON hr_leave_requests(employee_id);

-- ----------------------------------------------------------------------------
-- HR Shifts - shift definitions per organization
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_shifts (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    shift_name      VARCHAR(100) NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    break_minutes   INTEGER DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hr_shift_org ON hr_shifts(organization_id);

-- ----------------------------------------------------------------------------
-- HR Employee Shifts - shift assignments per employee per day
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_employee_shifts (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    shift_id        BIGINT NOT NULL REFERENCES hr_shifts(id),
    shift_date      DATE NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, shift_date)
);

CREATE INDEX idx_hr_emp_shift_org_date ON hr_employee_shifts(organization_id, shift_date);

-- ----------------------------------------------------------------------------
-- HR Holidays - organization-specific holiday calendar
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_holidays (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES hr_organizations(id),
    holiday_name    VARCHAR(150) NOT NULL,
    holiday_date    DATE NOT NULL,
    is_optional     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hr_holiday_org_date ON hr_holidays(organization_id, holiday_date);

-- ----------------------------------------------------------------------------
-- HR Employee Documents
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_employee_documents (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    document_type   VARCHAR(50) NOT NULL,
    document_name   VARCHAR(200) NOT NULL,
    file_url        TEXT,
    file_key        VARCHAR(500),
    expiry_date     DATE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    uploaded_by     BIGINT
);

CREATE INDEX idx_hr_emp_doc_employee ON hr_employee_documents(employee_id);
CREATE INDEX idx_hr_emp_doc_expiry ON hr_employee_documents(organization_id, expiry_date);
