-- Fix 1: Migrate any existing data from old community_* governance tables (V232)
--        into the new governance_* entity tables (V253).
--        Old tables are retained as archive — drop them only after prod verification.

INSERT INTO governance_meeting (community_id, type, title, description, meeting_date, meeting_time, venue, status, created_at, updated_at, created_by)
SELECT
    community_id,
    COALESCE(type, 'AGM'),
    title,
    description,
    meeting_date,
    meeting_time,
    location,
    CASE status
        WHEN 'UPCOMING'   THEN 'SCHEDULED'
        WHEN 'COMPLETED'  THEN 'COMPLETED'
        WHEN 'CANCELLED'  THEN 'CANCELLED'
        ELSE 'SCHEDULED'
    END,
    created_at,
    updated_at,
    created_by
FROM community_meeting
ON CONFLICT DO NOTHING;

INSERT INTO governance_proposal (community_id, submitted_by, title, description, category, status, created_at, updated_at)
SELECT
    community_id,
    COALESCE(submitted_by_user_id, 0),
    title,
    description,
    COALESCE(category, 'GENERAL'),
    CASE status
        WHEN 'SUBMITTED'  THEN 'OPEN'
        WHEN 'APPROVED'   THEN 'APPROVED'
        WHEN 'REJECTED'   THEN 'REJECTED'
        ELSE 'OPEN'
    END,
    created_at,
    updated_at
FROM community_proposal
ON CONFLICT DO NOTHING;

INSERT INTO governance_resolution (community_id, resolution_number, title, passed_on, status, created_at, updated_at)
SELECT
    community_id,
    resolution_number,
    title,
    passed_date,
    COALESCE(status, 'PASSED'),
    created_at,
    updated_at
FROM community_resolution
ON CONFLICT DO NOTHING;

-- Fix 2: Add last_run_date column to maintenance_schedule for the scheduler job
ALTER TABLE maintenance_schedule
    ADD COLUMN IF NOT EXISTS last_run_date DATE NULL;
