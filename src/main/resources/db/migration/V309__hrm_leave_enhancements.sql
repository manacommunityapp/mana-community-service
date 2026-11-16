-- ============================================================================
-- HRM Leave Enhancements: half-day, multi-level approvals, comp-off, policies
-- ============================================================================

-- Policy fields on hr_leave_types
ALTER TABLE hr_leave_types
    ADD COLUMN IF NOT EXISTS approval_levels       INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS requires_hr_approval  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS min_notice_days       INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_consecutive_days  INTEGER,
    ADD COLUMN IF NOT EXISTS max_per_year          INTEGER,
    ADD COLUMN IF NOT EXISTS allow_negative_balance BOOLEAN NOT NULL DEFAULT FALSE;

-- Half-day and multi-level approval fields on hr_leave_requests
ALTER TABLE hr_leave_requests
    ADD COLUMN IF NOT EXISTS is_half_day          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS leave_session        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS approval_level       INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS l1_approved_by       BIGINT,
    ADD COLUMN IF NOT EXISTS l1_approved_at       TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS l1_remarks           TEXT;

CREATE INDEX IF NOT EXISTS idx_hr_leave_req_l1 ON hr_leave_requests(l1_approved_by);
