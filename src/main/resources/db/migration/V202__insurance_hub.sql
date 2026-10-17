-- Insurance Hub (partner-led MVP). See docs/insurance/*.md
-- Principles: no card data; minimal PII; append-only history/audit; idempotent partner events.

CREATE TABLE IF NOT EXISTS insurance_partner (
    id                         BIGSERIAL PRIMARY KEY,
    code                       VARCHAR(40)  NOT NULL UNIQUE,
    legal_name                 VARCHAR(200) NOT NULL,
    partner_type               VARCHAR(30)  NOT NULL,            -- INSURER | IRDAI_INTERMEDIARY | REFERRAL_PARTNER
    irdai_registration_no      VARCHAR(60),
    authorisation_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    authorisation_verified_at  TIMESTAMP,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVAL | ACTIVE | SUSPENDED
    handoff_base_url           VARCHAR(500),                     -- https only; validated server-side
    disclosure_text            VARCHAR(2000),
    commercial_disclosure      VARCHAR(2000),                    -- partner relationship / remuneration disclosure
    created_by                 BIGINT       NOT NULL,
    approved_by                BIGINT,                           -- maker-checker: must differ from created_by
    created_at                 TIMESTAMP    NOT NULL DEFAULT NOW(),
    approved_at                TIMESTAMP,
    CONSTRAINT chk_partner_maker_checker CHECK (approved_by IS NULL OR approved_by <> created_by)
);

CREATE TABLE IF NOT EXISTS insurance_product (
    id          BIGSERIAL PRIMARY KEY,
    partner_id  BIGINT       NOT NULL REFERENCES insurance_partner(id),
    category    VARCHAR(30)  NOT NULL,   -- HEALTH FAMILY_FLOATER PARENTS LIFE_TERM MOTOR_CAR MOTOR_TWO_WHEELER HOME TRAVEL PERSONAL_ACCIDENT OTHER
    subtype     VARCHAR(60),
    name        VARCHAR(200) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ins_product_cat ON insurance_product (category, status);
CREATE INDEX IF NOT EXISTS idx_ins_product_partner ON insurance_product (partner_id);

CREATE TABLE IF NOT EXISTS insurance_product_version (
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT       NOT NULL REFERENCES insurance_product(id),
    version_no           INTEGER      NOT NULL,
    data_source          VARCHAR(20)  NOT NULL,   -- PARTNER_FEED | MANUAL_ENTRY | SANDBOX
    verification_status  VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED', -- UNVERIFIED | PENDING_APPROVAL | VERIFIED | REJECTED
    brochure_url         VARCHAR(500),
    policy_wording_url   VARCHAR(500),
    eligibility_json     TEXT,
    coverage_json        TEXT,    -- sum insured options, frequencies
    benefits_json        TEXT,
    waiting_periods_json TEXT,
    exclusions_json      TEXT,    -- exclusions, sub-limits, co-pay, deductibles, room-rent
    network_json         TEXT,
    disclosures_json     TEXT,
    submitted_by         BIGINT       NOT NULL,
    approved_by          BIGINT,
    verified_at          TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ins_pv UNIQUE (product_id, version_no),
    CONSTRAINT chk_pv_maker_checker CHECK (approved_by IS NULL OR approved_by <> submitted_by)
);

CREATE TABLE IF NOT EXISTS insurance_consent_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES app_user(id),
    purpose         VARCHAR(40)  NOT NULL,  -- QUOTE_SHARING | POLICY_STORAGE | CLAIMS_ASSISTANCE | GROUP_ENQUIRY | MARKETING
    partner_id      BIGINT REFERENCES insurance_partner(id),
    notice_version  VARCHAR(20)  NOT NULL,
    event_type      VARCHAR(10)  NOT NULL,  -- GRANTED | WITHDRAWN (append-only; latest event wins)
    granted_ref_id  BIGINT,                 -- for WITHDRAWN: the GRANTED row withdrawn
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ins_consent_user ON insurance_consent_record (user_id, purpose, created_at DESC);

CREATE TABLE IF NOT EXISTS insurance_quote_request (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES app_user(id),
    community_id      BIGINT,
    category          VARCHAR(30)  NOT NULL,
    partner_id        BIGINT REFERENCES insurance_partner(id),
    consent_id        BIGINT       NOT NULL REFERENCES insurance_consent_record(id),
    idempotency_key   VARCHAR(80)  NOT NULL,
    request_hash      VARCHAR(64)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,  -- SUBMITTED | QUOTED | PARTNER_UNAVAILABLE | FAILED | CANCELLED
    failure_code      VARCHAR(40),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ins_qr_idem UNIQUE (user_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS insurance_quote (
    id                 BIGSERIAL PRIMARY KEY,
    quote_request_id   BIGINT       NOT NULL REFERENCES insurance_quote_request(id),
    partner_id         BIGINT REFERENCES insurance_partner(id),   -- null for SANDBOX quotes
    product_version_id BIGINT REFERENCES insurance_product_version(id),
    partner_quote_ref  VARCHAR(80),
    data_source        VARCHAR(20)  NOT NULL,  -- PARTNER | SANDBOX
    premium_amount     NUMERIC(12,2) NOT NULL,
    premium_frequency  VARCHAR(20)  NOT NULL,
    sum_insured        NUMERIC(14,2),
    quoted_at          TIMESTAMP    NOT NULL,
    valid_until        TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ins_quote_req ON insurance_quote (quote_request_id);

CREATE TABLE IF NOT EXISTS insurance_application (
    id            BIGSERIAL PRIMARY KEY,
    quote_id      BIGINT REFERENCES insurance_quote(id),
    user_id       BIGINT NOT NULL REFERENCES app_user(id),
    partner_id    BIGINT NOT NULL REFERENCES insurance_partner(id),
    status        VARCHAR(30) NOT NULL,       -- HANDED_OFF | ISSUED | FAILED  (only partner-confirmed transitions)
    partner_ref   VARCHAR(80),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS insurance_policy (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT       NOT NULL REFERENCES app_user(id),
    community_id            BIGINT,
    partner_id              BIGINT REFERENCES insurance_partner(id),
    insurer_name            VARCHAR(200) NOT NULL,
    product_name            VARCHAR(200) NOT NULL,
    category                VARCHAR(30)  NOT NULL,
    policy_number_last4     VARCHAR(4),
    policy_number_enc       TEXT,                    -- AES-GCM via FieldEncryptionService; null when encryption disabled
    data_source             VARCHAR(20)  NOT NULL,   -- USER_DECLARED | PARTNER_VERIFIED
    start_date              DATE,
    expiry_date             DATE,
    sum_insured             NUMERIC(14,2),
    premium_amount          NUMERIC(12,2),
    premium_frequency       VARCHAR(20),
    status                  VARCHAR(20)  NOT NULL,   -- ACTIVE_DECLARED | ACTIVE_VERIFIED | EXPIRED | LAPSED | CANCELLED
    nominee_name            VARCHAR(120),
    insurer_contact         VARCHAR(300),
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ins_policy_user ON insurance_policy (user_id);
CREATE INDEX IF NOT EXISTS idx_ins_policy_expiry ON insurance_policy (expiry_date);

CREATE TABLE IF NOT EXISTS insured_member (
    id          BIGSERIAL PRIMARY KEY,
    policy_id   BIGINT NOT NULL REFERENCES insurance_policy(id) ON DELETE CASCADE,
    full_name   VARCHAR(120) NOT NULL,
    relationship VARCHAR(30),
    date_of_birth DATE
);

CREATE TABLE IF NOT EXISTS insurance_policy_document (
    id              BIGSERIAL PRIMARY KEY,
    policy_id       BIGINT NOT NULL REFERENCES insurance_policy(id),
    owner_user_id   BIGINT NOT NULL REFERENCES app_user(id),
    doc_type        VARCHAR(30) NOT NULL,       -- POLICY | RECEIPT | CLAIM_SUPPORT
    storage_key     VARCHAR(300) NOT NULL,      -- private bucket key; never a public URL
    content_type    VARCHAR(80)  NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    scan_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | CLEAN | INFECTED
    retention_until DATE,
    uploaded_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Immutable status history for policies (insurer/financial events are idempotent via event_id)
CREATE TABLE IF NOT EXISTS insurance_policy_status_history (
    id          BIGSERIAL PRIMARY KEY,
    policy_id   BIGINT      NOT NULL REFERENCES insurance_policy(id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    source      VARCHAR(20) NOT NULL,   -- USER | PARTNER_WEBHOOK | SYSTEM
    event_id    VARCHAR(80),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ins_psh_event UNIQUE (policy_id, event_id)
);

CREATE TABLE IF NOT EXISTS insurance_renewal_event (
    id             BIGSERIAL PRIMARY KEY,
    policy_id      BIGINT      NOT NULL REFERENCES insurance_policy(id),
    event_type     VARCHAR(30) NOT NULL,   -- UPCOMING_EXPIRY | OVERDUE | PAYMENT_EVENT | STATUS_CHANGE
    scheduled_for  DATE        NOT NULL,
    dedupe_key     VARCHAR(120) NOT NULL UNIQUE,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER     NOT NULL DEFAULT 0,
    last_error     VARCHAR(200),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Mana assistance tickets. NOT insurer claims. insurer_* fields are user-declared or partner-confirmed only.
CREATE TABLE IF NOT EXISTS insurance_claims_assistance_case (
    id                       BIGSERIAL PRIMARY KEY,
    case_ref                 VARCHAR(24)  NOT NULL UNIQUE,
    user_id                  BIGINT       NOT NULL REFERENCES app_user(id),
    community_id             BIGINT,
    policy_id                BIGINT REFERENCES insurance_policy(id),
    claim_type               VARCHAR(30)  NOT NULL,
    description              VARCHAR(1000),
    status                   VARCHAR(20)  NOT NULL DEFAULT 'OPEN',   -- Mana-side status only
    assigned_team            VARCHAR(60),
    insurer_claim_reference  VARCHAR(80),
    insurer_claim_reference_source VARCHAR(20),                      -- USER_DECLARED | PARTNER_CONFIRMED
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ins_case_user ON insurance_claims_assistance_case (user_id);

CREATE TABLE IF NOT EXISTS insurance_group_enquiry (
    id                  BIGSERIAL PRIMARY KEY,
    enquiry_ref         VARCHAR(24)  NOT NULL UNIQUE,
    user_id             BIGINT       NOT NULL REFERENCES app_user(id),
    community_id        BIGINT,
    organisation_name   VARCHAR(200) NOT NULL,
    organisation_type   VARCHAR(30)  NOT NULL, -- APARTMENT_ASSOCIATION SCHOOL COLLEGE ALUMNI BUSINESS
    coverage_scope      VARCHAR(30)  NOT NULL, -- COMMON_PROPERTY | INDIVIDUAL_RESIDENT | GROUP_POLICY
    coverage_sought     VARCHAR(40)  NOT NULL,
    approx_members      INTEGER      NOT NULL,
    requirements        VARCHAR(1000),
    contact_method      VARCHAR(10)  NOT NULL, -- PHONE | EMAIL
    consent_id          BIGINT       NOT NULL REFERENCES insurance_consent_record(id),
    status              VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    assigned_to         BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ins_genq_user ON insurance_group_enquiry (user_id);

CREATE TABLE IF NOT EXISTS insurance_partner_webhook_event (
    id               BIGSERIAL PRIMARY KEY,
    partner_id       BIGINT REFERENCES insurance_partner(id),
    partner_code     VARCHAR(40) NOT NULL,
    event_id         VARCHAR(80),
    signature_valid  BOOLEAN     NOT NULL,
    payload_sha256   VARCHAR(64) NOT NULL,     -- body is NOT stored (may contain PII)
    event_type       VARCHAR(40),
    outcome          VARCHAR(30) NOT NULL,     -- PROCESSED | DUPLICATE | REJECTED_SIGNATURE | REJECTED_STALE | FAILED
    received_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ins_wh_event ON insurance_partner_webhook_event (partner_code, event_id)
    WHERE outcome IN ('PROCESSED');
CREATE INDEX IF NOT EXISTS idx_ins_wh_outcome ON insurance_partner_webhook_event (outcome, received_at DESC);

-- Tamper-evident audit: hash chain + UPDATE/DELETE blocked by trigger.
CREATE TABLE IF NOT EXISTS insurance_audit_event (
    id           BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    action       VARCHAR(60)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    VARCHAR(40),
    detail       VARCHAR(500),               -- never PII / health data / secrets
    prev_hash    VARCHAR(64),
    hash         VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ins_audit_entity ON insurance_audit_event (entity_type, entity_id);

CREATE OR REPLACE FUNCTION insurance_block_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'insurance append-only table: % not allowed on %', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ins_audit_immutable ON insurance_audit_event;
CREATE TRIGGER trg_ins_audit_immutable BEFORE UPDATE OR DELETE ON insurance_audit_event
    FOR EACH ROW EXECUTE FUNCTION insurance_block_mutation();

DROP TRIGGER IF EXISTS trg_ins_psh_immutable ON insurance_policy_status_history;
CREATE TRIGGER trg_ins_psh_immutable BEFORE UPDATE OR DELETE ON insurance_policy_status_history
    FOR EACH ROW EXECUTE FUNCTION insurance_block_mutation();

DROP TRIGGER IF EXISTS trg_ins_consent_immutable ON insurance_consent_record;
CREATE TRIGGER trg_ins_consent_immutable BEFORE UPDATE OR DELETE ON insurance_consent_record
    FOR EACH ROW EXECUTE FUNCTION insurance_block_mutation();
