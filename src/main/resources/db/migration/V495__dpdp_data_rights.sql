-- V495: DPDP Data Rights Request table supporting both platform DPDP and privacy modules
CREATE TABLE IF NOT EXISTS data_rights_requests (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT,
    resident_id         BIGINT,
    community_id        BIGINT,
    user_email          VARCHAR(255),
    right_type          VARCHAR(50),
    request_type        VARCHAR(50),
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    description         VARCHAR(2000),
    reason              TEXT,
    correction_details  VARCHAR(1000),
    admin_notes         VARCHAR(1000),
    due_by              TIMESTAMPTZ,
    processed_by        BIGINT,
    requested_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ,
    resolved_by         BIGINT,
    resolution_notes    TEXT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Ensure all columns exist if the table was created by an earlier entity or schema update
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS resident_id        BIGINT;
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS community_id       BIGINT;
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS request_type       VARCHAR(50);
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS reason             TEXT;
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS requested_at       TIMESTAMPTZ DEFAULT now();
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS resolved_at        TIMESTAMPTZ;
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS resolved_by        BIGINT;
ALTER TABLE data_rights_requests ADD COLUMN IF NOT EXISTS resolution_notes   TEXT;

-- Relax NOT NULL constraints on legacy/alternative columns so both entity writers succeed
ALTER TABLE data_rights_requests ALTER COLUMN user_id    DROP NOT NULL;
ALTER TABLE data_rights_requests ALTER COLUMN user_email DROP NOT NULL;
ALTER TABLE data_rights_requests ALTER COLUMN right_type DROP NOT NULL;
ALTER TABLE data_rights_requests ALTER COLUMN updated_at DROP NOT NULL;

-- Backfill cross-compatible columns
UPDATE data_rights_requests SET resident_id = user_id WHERE resident_id IS NULL AND user_id IS NOT NULL;
UPDATE data_rights_requests SET user_id = resident_id WHERE user_id IS NULL AND resident_id IS NOT NULL;
UPDATE data_rights_requests SET request_type = right_type WHERE request_type IS NULL AND right_type IS NOT NULL;
UPDATE data_rights_requests SET right_type = request_type WHERE right_type IS NULL AND request_type IS NOT NULL;
UPDATE data_rights_requests SET requested_at = created_at WHERE requested_at IS NULL AND created_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_drr_resident  ON data_rights_requests(resident_id, status);
CREATE INDEX IF NOT EXISTS idx_drr_community ON data_rights_requests(community_id, status);
CREATE INDEX IF NOT EXISTS idx_drr_user      ON data_rights_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_drr_status    ON data_rights_requests(status);

COMMENT ON COLUMN data_rights_requests.request_type IS 'ERASURE (DPDP s.12) | CORRECTION | PORTABILITY | RESTRICTION';
COMMENT ON COLUMN data_rights_requests.status IS 'PENDING | IN_REVIEW | COMPLETED | REJECTED';
