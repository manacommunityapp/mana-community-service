-- ─────────────────────────────────────────────────────────────────
-- V204 · Sports External Registration Forms (Google Forms & Mana Forms)
--   Tables: sports_external_form, sports_external_form_field_mapping,
--           sports_external_response, sports_external_sync_state
--   Enhancements: sports_event_registration (source, phone, external_response_id)
-- ─────────────────────────────────────────────────────────────────

-- 1. Extend sports_event_registration with source attribution & contact tracking
ALTER TABLE sports_event_registration
    ADD COLUMN IF NOT EXISTS registration_source VARCHAR(32) NOT NULL DEFAULT 'MANA_NATIVE',
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS external_response_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS external_form_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_external_guest BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_reg_ext_resp
    ON sports_event_registration(external_response_id);

-- 2. External form integration master table
CREATE TABLE IF NOT EXISTS sports_external_form (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            BIGINT       NOT NULL REFERENCES sports_event(id) ON DELETE CASCADE,
    community_id        BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    provider            VARCHAR(32)  NOT NULL DEFAULT 'GOOGLE_FORMS',
    external_form_id    VARCHAR(255) NOT NULL,
    responder_url       VARCHAR(1024) NOT NULL,
    edit_url            VARCHAR(1024),
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    webhook_token       VARCHAR(255) UNIQUE,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    approval_mode       VARCHAR(32)  NOT NULL DEFAULT 'TOURNAMENT_DEFAULT',
    created_by_user_id  BIGINT       REFERENCES app_user(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sef_event ON sports_external_form(event_id);
CREATE INDEX IF NOT EXISTS idx_sef_community ON sports_external_form(community_id);
CREATE INDEX IF NOT EXISTS idx_sef_token ON sports_external_form(webhook_token);

-- 3. Question-to-field mapping table
CREATE TABLE IF NOT EXISTS sports_external_form_field_mapping (
    id                   BIGSERIAL PRIMARY KEY,
    form_id              BIGINT       NOT NULL REFERENCES sports_external_form(id) ON DELETE CASCADE,
    mana_field_key       VARCHAR(64)  NOT NULL,
    external_question_id VARCHAR(255) NOT NULL,
    question_title       VARCHAR(255) NOT NULL,
    mapping_type         VARCHAR(32)  NOT NULL DEFAULT 'DIRECT',
    mapping_rules_json   TEXT,
    required             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sefm_form ON sports_external_form_field_mapping(form_id);

-- 4. Staging table for ingested external responses
CREATE TABLE IF NOT EXISTS sports_external_response (
    id                     BIGSERIAL PRIMARY KEY,
    form_id                BIGINT       NOT NULL REFERENCES sports_external_form(id) ON DELETE CASCADE,
    external_response_id   VARCHAR(255) NOT NULL,
    respondent_email       VARCHAR(255),
    submitted_at           TIMESTAMPTZ  NOT NULL,
    raw_payload_json       TEXT         NOT NULL,
    status                 VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED',
    review_reason          VARCHAR(255),
    review_notes           TEXT,
    validation_errors      TEXT,
    linked_registration_id BIGINT       REFERENCES sports_event_registration(id) ON DELETE SET NULL,
    resolved_user_id       BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    reviewed_by_user_id    BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    reviewed_at            TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_sef_resp UNIQUE (form_id, external_response_id)
);

CREATE INDEX IF NOT EXISTS idx_ser_form_status ON sports_external_response(form_id, status);
CREATE INDEX IF NOT EXISTS idx_ser_email ON sports_external_response(respondent_email);

-- 5. Synchronization state tracking
CREATE TABLE IF NOT EXISTS sports_external_sync_state (
    id                     BIGSERIAL PRIMARY KEY,
    form_id                BIGINT       NOT NULL UNIQUE REFERENCES sports_external_form(id) ON DELETE CASCADE,
    sync_mode              VARCHAR(32)  NOT NULL DEFAULT 'POLLING',
    watch_id               VARCHAR(255),
    watch_resource_id      VARCHAR(255),
    watch_expiration       TIMESTAMPTZ,
    last_sync_timestamp    TIMESTAMPTZ,
    total_responses_polled INT          NOT NULL DEFAULT 0,
    total_imported         INT          NOT NULL DEFAULT 0,
    total_quarantined      INT          NOT NULL DEFAULT 0,
    total_rejected         INT          NOT NULL DEFAULT 0,
    sync_status            VARCHAR(32)  NOT NULL DEFAULT 'IDLE',
    sync_error             TEXT,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);
