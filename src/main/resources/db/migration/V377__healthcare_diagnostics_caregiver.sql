-- Healthcare: diagnostics partner SLA + caregiver-consent workflows

CREATE TABLE diagnostics_sla (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL,
    partner_id          BIGINT NOT NULL,
    partner_name        VARCHAR(300) NOT NULL,
    test_category       VARCHAR(200) NOT NULL,
    sla_hours           INT NOT NULL,
    report_due_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    report_delivered_at TIMESTAMP WITH TIME ZONE,
    order_id            BIGINT NOT NULL,
    patient_user_id     BIGINT NOT NULL,
    breach              BOOLEAN NOT NULL DEFAULT FALSE,
    breach_notified     BOOLEAN NOT NULL DEFAULT FALSE,
    escalated           BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_at        TIMESTAMP WITH TIME ZONE,
    notes               VARCHAR(1000),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_diag_sla_community ON diagnostics_sla(community_id);
CREATE INDEX idx_diag_sla_partner ON diagnostics_sla(partner_id);
CREATE INDEX idx_diag_sla_due ON diagnostics_sla(report_due_at);
CREATE INDEX idx_diag_sla_breach ON diagnostics_sla(breach) WHERE breach = TRUE;

CREATE TABLE caregiver_consent (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL,
    patient_user_id     BIGINT NOT NULL,
    caregiver_user_id   BIGINT NOT NULL,
    caregiver_name      VARCHAR(300) NOT NULL,
    relationship        VARCHAR(100) NOT NULL,
    consent_scope       VARCHAR(500) NOT NULL,
    valid_from          DATE NOT NULL,
    valid_until         DATE,
    revoked             BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at          TIMESTAMP WITH TIME ZONE,
    revoked_by          BIGINT,
    document_url        VARCHAR(1000),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_consent_community ON caregiver_consent(community_id);
CREATE INDEX idx_consent_patient ON caregiver_consent(patient_user_id);
CREATE INDEX idx_consent_caregiver ON caregiver_consent(caregiver_user_id);

CREATE TABLE doctor_slot_utilization (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    doctor_id       BIGINT NOT NULL,
    slot_date       DATE NOT NULL,
    total_slots     INT NOT NULL DEFAULT 0,
    booked_slots    INT NOT NULL DEFAULT 0,
    attended_slots  INT NOT NULL DEFAULT 0,
    no_show_slots   INT NOT NULL DEFAULT 0,
    cancelled_slots INT NOT NULL DEFAULT 0,
    calculated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, doctor_id, slot_date)
);
CREATE INDEX idx_util_community ON doctor_slot_utilization(community_id);
CREATE INDEX idx_util_doctor ON doctor_slot_utilization(doctor_id);
