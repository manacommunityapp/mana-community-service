-- ============================================================================
-- HRM Leave Enhancements Migration
-- Adds half-day leave, multi-level approvals, carry-forward, comp-off support,
-- and policy-based approval fields
-- ============================================================================

-- Half-day leave support on leave requests
ALTER TABLE hr_leave_requests
    ADD COLUMN IF NOT EXISTS is_half_day BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS leave_session VARCHAR(20);

-- Multi-level approval tracking on leave requests
ALTER TABLE hr_leave_requests
    ADD COLUMN IF NOT EXISTS approval_level INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS l1_approved_by BIGINT,
    ADD COLUMN IF NOT EXISTS l1_approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS l1_remarks TEXT;

-- Multiple approval levels configuration on leave types
ALTER TABLE hr_leave_types
    ADD COLUMN IF NOT EXISTS approval_levels INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS requires_hr_approval BOOLEAN NOT NULL DEFAULT FALSE;

-- Policy-based approval fields on leave types
ALTER TABLE hr_leave_types
    ADD COLUMN IF NOT EXISTS min_notice_days INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_consecutive_days INTEGER,
    ADD COLUMN IF NOT EXISTS max_per_year INTEGER,
    ADD COLUMN IF NOT EXISTS allow_negative_balance BOOLEAN NOT NULL DEFAULT FALSE;

-- Index to speed up carry-forward and balance queries
CREATE INDEX IF NOT EXISTS idx_hr_leave_bal_type_year ON hr_leave_balances(leave_type_id, year);
