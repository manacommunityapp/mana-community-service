-- NOC (No Objection Certificate) requests issued by committee to residents
CREATE TABLE noc_requests (
    id               BIGSERIAL    PRIMARY KEY,
    community_id     BIGINT       NOT NULL,
    resident_id      BIGINT       NOT NULL,
    noc_type         VARCHAR(50)  NOT NULL,
    -- RENOVATION, PET_REGISTRATION, SUBLETTING, EVENT, OTHER
    description      TEXT         NOT NULL,
    supporting_docs  JSONB,       -- [{url, name}]
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    -- PENDING, UNDER_REVIEW, APPROVED, REJECTED, WITHDRAWN
    reviewed_by      BIGINT,
    reviewer_notes   TEXT,
    decided_at       TIMESTAMPTZ,
    noc_number       VARCHAR(100) UNIQUE,   -- issued on approval: NOC/2025/0001
    valid_until      DATE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_noc_requests_community ON noc_requests(community_id, status);
CREATE INDEX idx_noc_requests_resident  ON noc_requests(resident_id);
