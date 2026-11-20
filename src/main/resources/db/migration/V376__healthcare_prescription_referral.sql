-- Healthcare: prescription follow-up + referral transparency

CREATE TABLE prescription_followup (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL,
    appointment_id      BIGINT NOT NULL,
    patient_user_id     BIGINT NOT NULL,
    doctor_id           BIGINT NOT NULL,
    medication_name     VARCHAR(500) NOT NULL,
    dosage              VARCHAR(200),
    duration_days       INT,
    followup_due_date   DATE NOT NULL,
    followup_note       VARCHAR(1000),
    reminder_sent       BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_at    TIMESTAMP WITH TIME ZONE,
    completed           BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pfup_community ON prescription_followup(community_id);
CREATE INDEX idx_pfup_patient ON prescription_followup(patient_user_id);
CREATE INDEX idx_pfup_due ON prescription_followup(followup_due_date);

CREATE TABLE referral (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL,
    referring_doctor_id BIGINT NOT NULL,
    patient_user_id     BIGINT NOT NULL,
    referred_to_name    VARCHAR(300) NOT NULL,
    referred_to_type    VARCHAR(100) NOT NULL,
    specialization      VARCHAR(200),
    reason              VARCHAR(1000) NOT NULL,
    urgency             VARCHAR(50) NOT NULL DEFAULT 'ROUTINE',
    referral_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    appointment_booked  BOOLEAN NOT NULL DEFAULT FALSE,
    appointment_date    DATE,
    outcome_notes       VARCHAR(1000),
    feedback_to_referrer VARCHAR(1000),
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_referral_community ON referral(community_id);
CREATE INDEX idx_referral_doctor ON referral(referring_doctor_id);
CREATE INDEX idx_referral_patient ON referral(patient_user_id);
