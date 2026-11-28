-- ============================================================================
-- V520: Schema Precision, Missing Primary Keys, & Table Collision Fixes
-- Fixes remaining critical findings from database architecture audit:
--   1. Dedicated manacommunity.safety_guard_shift table to resolve entity
--      collision with V140 guard.entity.GuardShift on manacommunity.guard_shift.
--   2. Surrogate PRIMARY KEY on 5 active collection tables + composite PRIMARY
--      KEY (id, reading_at) on partitioned manacommunity.sensor_telemetry.
--   3. Convert DOUBLE PRECISION monetary/financial columns on commute_ride and
--      HR payroll/expense/recruitment tables to exact NUMERIC(14,2)/(14,4).
-- ============================================================================

-- 1. Dedicated table for com.manacommunity.api.safety.model.GuardShift
CREATE TABLE IF NOT EXISTS manacommunity.safety_guard_shift (
    id BIGSERIAL PRIMARY KEY,
    guard_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    area VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_DUTY',
    checkpoints_completed INTEGER NOT NULL DEFAULT 0,
    total_checkpoints INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_safety_guard_shift_guard
    ON manacommunity.safety_guard_shift (guard_id);
CREATE INDEX IF NOT EXISTS idx_safety_guard_shift_community
    ON manacommunity.safety_guard_shift (community_id);
CREATE INDEX IF NOT EXISTS idx_safety_guard_shift_status
    ON manacommunity.safety_guard_shift (status);

-- 2. Add missing Primary Keys on collection tables and partitioned telemetry
DO $$
DECLARE
    v_tbl TEXT;
    v_tables TEXT[] := ARRAY[
        'community_proposal_pros',
        'community_proposal_cons',
        'community_resolution_provisions',
        'event_pooja_seva_start_times',
        'staff_job_post_requirements'
    ];
BEGIN
    FOREACH v_tbl IN ARRAY v_tables LOOP
        IF to_regclass('manacommunity.' || v_tbl) IS NOT NULL
           AND NOT EXISTS (
               SELECT 1
               FROM pg_constraint
               WHERE conrelid = to_regclass('manacommunity.' || v_tbl)
                 AND contype = 'p'
           )
           AND NOT EXISTS (
               SELECT 1
               FROM information_schema.columns
               WHERE table_schema = 'manacommunity'
                 AND table_name = v_tbl
                 AND column_name = 'id'
           ) THEN
            EXECUTE format('ALTER TABLE manacommunity.%I ADD COLUMN id BIGSERIAL PRIMARY KEY', v_tbl);
        END IF;
    END LOOP;

    IF to_regclass('manacommunity.sensor_telemetry') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint
           WHERE conrelid = to_regclass('manacommunity.sensor_telemetry')
             AND contype = 'p'
       )
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'manacommunity'
             AND table_name = 'sensor_telemetry'
             AND column_name = 'id'
       )
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'manacommunity'
             AND table_name = 'sensor_telemetry'
             AND column_name = 'reading_at'
       )
       AND NOT EXISTS (
           SELECT 1
           FROM manacommunity.sensor_telemetry
           GROUP BY id, reading_at
           HAVING COUNT(*) > 1
       ) THEN
        ALTER TABLE manacommunity.sensor_telemetry ADD PRIMARY KEY (id, reading_at);
    END IF;
END $$;

-- 3. Convert DOUBLE PRECISION financial columns to exact NUMERIC(14,2) / NUMERIC(14,4)
DO $$
DECLARE
    rec RECORD;
    v_data_type TEXT;
    v_default TEXT;
BEGIN
    FOR rec IN
        SELECT * FROM (VALUES
            ('commute_ride',         'price_per_seat',            14, 2, false),
            ('hr_salary_components', 'default_value',             14, 4, false),
            ('hr_salary_structures', 'ctc_annual',                14, 2, false),
            ('hr_salary_structures', 'basic_monthly',             14, 2, false),
            ('hr_salary_structures', 'hra_monthly',               14, 2, true),
            ('hr_salary_structures', 'da_monthly',                14, 2, true),
            ('hr_salary_structures', 'special_allowance_monthly', 14, 2, true),
            ('hr_salary_structures', 'other_allowances_monthly',  14, 2, true),
            ('hr_salary_structures', 'pf_employee_monthly',       14, 2, true),
            ('hr_salary_structures', 'pf_employer_monthly',       14, 2, true),
            ('hr_salary_structures', 'esi_employee_monthly',      14, 2, true),
            ('hr_salary_structures', 'esi_employer_monthly',      14, 2, true),
            ('hr_salary_structures', 'professional_tax_monthly',  14, 2, true),
            ('hr_salary_structures', 'tds_monthly',               14, 2, true),
            ('hr_payroll_runs',      'total_gross',               14, 2, true),
            ('hr_payroll_runs',      'total_deductions',          14, 2, true),
            ('hr_payroll_runs',      'total_net',                 14, 2, true),
            ('hr_payslips',          'basic_salary',              14, 2, true),
            ('hr_payslips',          'hra',                       14, 2, true),
            ('hr_payslips',          'da',                        14, 2, true),
            ('hr_payslips',          'special_allowance',         14, 2, true),
            ('hr_payslips',          'other_allowances',          14, 2, true),
            ('hr_payslips',          'gross_salary',              14, 2, true),
            ('hr_payslips',          'pf_employee',               14, 2, true),
            ('hr_payslips',          'pf_employer',               14, 2, true),
            ('hr_payslips',          'esi_employee',              14, 2, true),
            ('hr_payslips',          'esi_employer',              14, 2, true),
            ('hr_payslips',          'professional_tax',          14, 2, true),
            ('hr_payslips',          'tds',                       14, 2, true),
            ('hr_payslips',          'other_deductions',          14, 2, true),
            ('hr_payslips',          'total_deductions',          14, 2, true),
            ('hr_payslips',          'net_salary',                14, 2, true),
            ('hr_payslips',          'reimbursements_paid',       14, 2, true),
            ('hr_expense_claims',    'total_amount',              14, 2, true),
            ('hr_expense_claims',    'approved_amount',           14, 2, false),
            ('hr_expense_items',     'amount',                    14, 2, false),
            ('hr_travel_requests',   'estimated_cost',            14, 2, false),
            ('hr_travel_requests',   'advance_requested',         14, 2, true),
            ('hr_applicants',        'expected_salary',           14, 2, false),
            ('hr_offers',            'offered_salary',            14, 2, false)
        ) AS t(tbl, col, prec, scl, has_zero_default)
    LOOP
        SELECT data_type, column_default
        INTO v_data_type, v_default
        FROM information_schema.columns
        WHERE table_schema = 'manacommunity'
          AND table_name = rec.tbl
          AND column_name = rec.col;

        IF v_data_type IN ('double precision', 'real') THEN
            IF v_default IS NOT NULL THEN
                EXECUTE format('ALTER TABLE manacommunity.%I ALTER COLUMN %I DROP DEFAULT', rec.tbl, rec.col);
            END IF;

            EXECUTE format(
                'ALTER TABLE manacommunity.%I ALTER COLUMN %I TYPE NUMERIC(%s,%s) USING ROUND(%I::numeric, %s)',
                rec.tbl, rec.col, rec.prec, rec.scl, rec.col, rec.scl
            );

            IF rec.has_zero_default THEN
                EXECUTE format('ALTER TABLE manacommunity.%I ALTER COLUMN %I SET DEFAULT 0', rec.tbl, rec.col);
            END IF;
        END IF;
    END LOOP;
END $$;
