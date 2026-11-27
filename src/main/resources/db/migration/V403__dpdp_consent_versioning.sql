-- DPDP Act compliance: purpose-granular consent, versioned privacy notices, append-only records

CREATE TABLE IF NOT EXISTS privacy_notice_version (
    id              BIGSERIAL       PRIMARY KEY,
    version_code    VARCHAR(20)     NOT NULL UNIQUE,  -- e.g. 'v1.0', 'v1.1'
    effective_date  DATE            NOT NULL,
    content_hash    VARCHAR(64)     NOT NULL,          -- SHA-256 of notice text
    notice_url      VARCHAR(2048),
    is_current      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'consent_purpose_code') THEN
        CREATE TYPE consent_purpose_code AS ENUM (
            'SERVICE_OPERATION',
            'HEALTHCARE_DATA',
            'ANALYTICS',
            'MARKETING',
            'THIRD_PARTY_SHARING',
            'PROFILE_ENHANCEMENT',
            'PUSH_NOTIFICATIONS'
        );
    END IF;
END $$;

-- Append-only consent record (DPDP §6 — specific, informed, unconditional consent)
CREATE TABLE IF NOT EXISTS user_consent_record (
    id                      BIGSERIAL               PRIMARY KEY,
    record_id               UUID                    NOT NULL DEFAULT gen_random_uuid(),
    user_id                 BIGINT                  NOT NULL REFERENCES app_user(id),
    community_id            BIGINT                  REFERENCES community(id),
    purpose                 consent_purpose_code    NOT NULL,
    privacy_notice_version  VARCHAR(20)             NOT NULL,
    action                  VARCHAR(10)             NOT NULL CHECK (action IN ('GRANT', 'REVOKE')),
    actor_id                BIGINT,                 -- user_id of who performed the action
    ip_address              INET,
    user_agent              VARCHAR(512),
    created_at              TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

-- Append-only at DB level
CREATE OR REPLACE RULE no_update_user_consent AS ON UPDATE TO user_consent_record DO INSTEAD NOTHING;
CREATE OR REPLACE RULE no_delete_user_consent AS ON DELETE TO user_consent_record DO INSTEAD NOTHING;

CREATE INDEX IF NOT EXISTS idx_consent_user_purpose      ON user_consent_record(user_id, purpose, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_consent_record_community  ON user_consent_record(community_id, created_at DESC);

-- Data rights requests (DPDP §13 — right to access, correct, erase, nominate)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'data_rights_request_type') THEN
        CREATE TYPE data_rights_request_type AS ENUM (
            'ACCESS', 'CORRECTION', 'ERASURE', 'NOMINATION', 'GRIEVANCE'
        );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'data_rights_request_status') THEN
        CREATE TYPE data_rights_request_status AS ENUM (
            'PENDING', 'IN_REVIEW', 'COMPLETED', 'REJECTED', 'WITHDRAWN'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS data_rights_request (
    id              BIGSERIAL                       PRIMARY KEY,
    request_id      UUID                            NOT NULL DEFAULT gen_random_uuid(),
    user_id         BIGINT                          NOT NULL REFERENCES app_user(id),
    request_type    data_rights_request_type        NOT NULL,
    status          data_rights_request_status      NOT NULL DEFAULT 'PENDING',
    description     TEXT,
    resolution      TEXT,
    resolved_at     TIMESTAMPTZ,
    resolved_by     BIGINT,
    due_date        DATE,                           -- 72-hour SLA for ERASURE/ACCESS
    created_at      TIMESTAMPTZ                     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ                     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rights_request_user   ON data_rights_request(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_rights_request_status ON data_rights_request(status, due_date);
