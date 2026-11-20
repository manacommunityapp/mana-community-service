-- Healthcare: appointment no-show management + provider credential expiry

CREATE TABLE appointment_no_show (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    appointment_id  BIGINT NOT NULL,
    doctor_id       BIGINT NOT NULL,
    patient_user_id BIGINT NOT NULL,
    no_show_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fee_charged     NUMERIC(12,2),
    fee_waived      BOOLEAN NOT NULL DEFAULT FALSE,
    waiver_reason   VARCHAR(500),
    reviewed_by     BIGINT,
    notes           VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_no_show_community ON appointment_no_show(community_id);
CREATE INDEX idx_no_show_doctor ON appointment_no_show(doctor_id);
CREATE INDEX idx_no_show_patient ON appointment_no_show(patient_user_id);

CREATE TABLE provider_credential (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    provider_id     BIGINT NOT NULL,
    credential_type VARCHAR(100) NOT NULL,
    credential_number VARCHAR(100),
    issued_by       VARCHAR(200),
    issued_date     DATE,
    expiry_date     DATE NOT NULL,
    document_url    VARCHAR(1000),
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by     BIGINT,
    verified_at     TIMESTAMP WITH TIME ZONE,
    alert_sent_30   BOOLEAN NOT NULL DEFAULT FALSE,
    alert_sent_7    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cred_community ON provider_credential(community_id);
CREATE INDEX idx_cred_provider ON provider_credential(provider_id);
CREATE INDEX idx_cred_expiry ON provider_credential(expiry_date);
