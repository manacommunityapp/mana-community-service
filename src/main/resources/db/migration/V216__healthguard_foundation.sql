-- HealthGuard Phase 1: Health Profiles, Emergency Contacts, Incidents, Notifications

CREATE TABLE manacommunity.health_profile (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    blood_group         VARCHAR(20) DEFAULT 'UNKNOWN',
    allergies           TEXT,
    medical_conditions  TEXT,
    emergency_notes     TEXT,
    height_cm           INTEGER,
    weight_kg           INTEGER,
    date_of_birth       DATE,
    organ_donor_consent BOOLEAN NOT NULL DEFAULT FALSE,
    medical_id_number   VARCHAR(100),
    physician_name      VARCHAR(150),
    physician_phone     VARCHAR(20),
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_health_profile_user ON manacommunity.health_profile(user_id);

CREATE TABLE manacommunity.emergency_contact (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    email               VARCHAR(200),
    relationship        VARCHAR(20) DEFAULT 'OTHER',
    priority            INTEGER NOT NULL DEFAULT 1,
    notify_on_sos       BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_alert     BOOLEAN NOT NULL DEFAULT TRUE,
    consent_given       BOOLEAN NOT NULL DEFAULT FALSE,
    consent_given_at    TIMESTAMP,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_emergency_contact_user ON manacommunity.emergency_contact(user_id, is_active);

CREATE TABLE manacommunity.emergency_incident (
    id               BIGSERIAL PRIMARY KEY,
    incident_uuid    VARCHAR(36) NOT NULL UNIQUE,
    user_id          BIGINT NOT NULL,
    severity         VARCHAR(40) NOT NULL DEFAULT 'LEVEL_3_POSSIBLE_EMERGENCY',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    latitude         NUMERIC(9,6),
    longitude        NUMERIC(9,6),
    location_address VARCHAR(300),
    description      TEXT,
    trigger_type     VARCHAR(30) DEFAULT 'MANUAL_SOS',
    triggered_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    acknowledged_at  TIMESTAMP,
    resolved_at      TIMESTAMP,
    notes            TEXT,
    alerts_sent      INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_emergency_incident_user ON manacommunity.emergency_incident(user_id, triggered_at DESC);
CREATE INDEX idx_emergency_incident_active ON manacommunity.emergency_incident(user_id, status) WHERE status = 'ACTIVE';

CREATE TABLE manacommunity.emergency_notification (
    id                BIGSERIAL PRIMARY KEY,
    incident_id       BIGINT NOT NULL REFERENCES manacommunity.emergency_incident(id),
    recipient_user_id BIGINT,
    recipient_phone   VARCHAR(20),
    recipient_name    VARCHAR(150),
    channel           VARCHAR(20) DEFAULT 'IN_APP',
    status            VARCHAR(20) DEFAULT 'PENDING',
    message           TEXT,
    sent_at           TIMESTAMP,
    delivered_at      TIMESTAMP,
    acknowledged_at   TIMESTAMP,
    failure_reason    VARCHAR(300),
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_emergency_notification_incident ON manacommunity.emergency_notification(incident_id);
CREATE INDEX idx_emergency_notification_recipient ON manacommunity.emergency_notification(recipient_user_id);
