-- HRM: Attendance correction records
CREATE TABLE IF NOT EXISTS hr_attendance_corrections (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     BIGINT NOT NULL,
    attendance_id       BIGINT NOT NULL REFERENCES hr_attendance(id),
    corrected_check_in  TIME,
    corrected_check_out TIME,
    corrected_status    VARCHAR(30),
    reason              VARCHAR(500) NOT NULL,
    corrected_by        BIGINT NOT NULL,
    original_check_in   TIME,
    original_check_out  TIME,
    original_status     VARCHAR(30),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_hr_att_correction_attendance ON hr_attendance_corrections(attendance_id);

-- Late arrival config on shifts
ALTER TABLE hr_shifts
    ADD COLUMN IF NOT EXISTS grace_period_minutes INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS late_threshold_minutes INTEGER NOT NULL DEFAULT 0;

-- LATE status is added to the AttendanceStatus enum (string column, no DB change needed)

-- Overtime columns on attendance
ALTER TABLE hr_attendance
    ADD COLUMN IF NOT EXISTS overtime_minutes INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overtime_approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS overtime_approved_by BIGINT;

-- Org-level working hours config
ALTER TABLE hr_organizations
    ADD COLUMN IF NOT EXISTS daily_work_hours INTEGER NOT NULL DEFAULT 8,
    ADD COLUMN IF NOT EXISTS overtime_threshold_minutes INTEGER NOT NULL DEFAULT 480,
    ADD COLUMN IF NOT EXISTS weekly_work_hours INTEGER NOT NULL DEFAULT 48;

-- Weekly timesheets
CREATE TABLE IF NOT EXISTS hr_timesheets (
    id                      BIGSERIAL PRIMARY KEY,
    organization_id         BIGINT NOT NULL,
    employee_id             BIGINT NOT NULL REFERENCES hr_employees(id),
    week_start_date         DATE NOT NULL,
    week_end_date           DATE NOT NULL,
    total_minutes           INTEGER NOT NULL DEFAULT 0,
    total_overtime_minutes  INTEGER NOT NULL DEFAULT 0,
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at            TIMESTAMPTZ,
    approved_by             BIGINT,
    approved_at             TIMESTAMPTZ,
    remarks                 VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (employee_id, week_start_date)
);
CREATE INDEX IF NOT EXISTS idx_hr_timesheet_org_status ON hr_timesheets(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_hr_timesheet_employee ON hr_timesheets(employee_id, week_start_date DESC);

-- Shift swap requests (3-step: request → colleague accept → manager approve)
CREATE TABLE IF NOT EXISTS hr_shift_swap_requests (
    id                      BIGSERIAL PRIMARY KEY,
    organization_id         BIGINT NOT NULL,
    requester_employee_id   BIGINT NOT NULL REFERENCES hr_employees(id),
    target_employee_id      BIGINT NOT NULL REFERENCES hr_employees(id),
    requester_shift_date    DATE NOT NULL,
    target_shift_date       DATE NOT NULL,
    reason                  VARCHAR(500),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING_COLLEAGUE',
    colleague_responded_at  TIMESTAMPTZ,
    manager_responded_at    TIMESTAMPTZ,
    approved_by             BIGINT,
    rejection_reason        VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_hr_shift_swap_requester ON hr_shift_swap_requests(requester_employee_id);
CREATE INDEX IF NOT EXISTS idx_hr_shift_swap_target ON hr_shift_swap_requests(target_employee_id, status);
CREATE INDEX IF NOT EXISTS idx_hr_shift_swap_org ON hr_shift_swap_requests(organization_id, status);
