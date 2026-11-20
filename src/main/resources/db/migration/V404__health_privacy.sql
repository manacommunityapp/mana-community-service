-- Health data access audit log and vendor DPA tracking

-- Every read of health/medical data is recorded for auditability
CREATE TABLE health_data_access_log (
    id              BIGSERIAL       PRIMARY KEY,
    log_id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    accessor_id     BIGINT          NOT NULL,         -- user or system accessing the data
    accessor_type   VARCHAR(50)     NOT NULL,         -- USER, SYSTEM, DOCTOR, ADMIN
    subject_user_id BIGINT          NOT NULL REFERENCES app_user(id),
    record_type     VARCHAR(100)    NOT NULL,         -- e.g. APPOINTMENT, PRESCRIPTION, REFERRAL
    record_id       VARCHAR(255)    NOT NULL,
    community_id    BIGINT          REFERENCES community(id),
    access_purpose  VARCHAR(200),
    ip_address      INET,
    correlation_id  VARCHAR(100),
    accessed_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Append-only at DB level
CREATE RULE no_update_health_access_log AS ON UPDATE TO health_data_access_log DO INSTEAD NOTHING;
CREATE RULE no_delete_health_access_log AS ON DELETE TO health_data_access_log DO INSTEAD NOTHING;

CREATE INDEX idx_health_access_subject  ON health_data_access_log(subject_user_id, accessed_at DESC);
CREATE INDEX idx_health_access_accessor ON health_data_access_log(accessor_id, accessed_at DESC);
CREATE INDEX idx_health_access_record   ON health_data_access_log(record_type, record_id);

-- Vendor Data Processing Agreements (DPDP §28 — Data Fiduciary must ensure DPA with processors)
CREATE TABLE vendor_dpa (
    id                  BIGSERIAL       PRIMARY KEY,
    vendor_name         VARCHAR(200)    NOT NULL,
    vendor_type         VARCHAR(100)    NOT NULL,  -- DIAGNOSTIC_LAB, PHARMACY, INSURANCE, etc.
    dpa_reference       VARCHAR(500)    NOT NULL,  -- contract reference or URL
    data_categories     JSONB,                     -- what data is shared
    purposes            JSONB,                     -- purposes for sharing
    effective_date      DATE            NOT NULL,
    expiry_date         DATE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id        BIGINT          REFERENCES community(id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vendor_dpa_active ON vendor_dpa(is_active, community_id);
