CREATE TABLE IF NOT EXISTS data_rights_requests (
    id              BIGSERIAL PRIMARY KEY,
    resident_id     BIGINT        NOT NULL,
    community_id    BIGINT        NOT NULL,
    request_type    VARCHAR(50)   NOT NULL,  -- ERASURE | CORRECTION | PORTABILITY | RESTRICTION
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    reason          TEXT,
    requested_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,
    resolved_by     BIGINT,
    resolution_notes TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_drr_resident ON data_rights_requests(resident_id, status);
CREATE INDEX IF NOT EXISTS idx_drr_community ON data_rights_requests(community_id, status);

COMMENT ON COLUMN data_rights_requests.request_type IS 'ERASURE (DPDP s.12) | CORRECTION | PORTABILITY | RESTRICTION';
COMMENT ON COLUMN data_rights_requests.status IS 'PENDING | IN_REVIEW | COMPLETED | REJECTED';
