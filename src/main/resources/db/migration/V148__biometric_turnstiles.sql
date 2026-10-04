-- Biometric & Facial Recognition Pedestrian Turnstiles Schema

CREATE TABLE IF NOT EXISTS manacommunity.biometric_turnstiles (
    id                     BIGSERIAL PRIMARY KEY,
    turnstile_identifier   VARCHAR(100)  NOT NULL UNIQUE, -- e.g. TS-GATE-NORTH-01
    turnstile_name         VARCHAR(150)  NOT NULL,
    community_id           BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    gate_location          VARCHAR(150)  NOT NULL,
    direction              VARCHAR(30)   NOT NULL DEFAULT 'BIDIRECTIONAL', -- ENTRY, EXIT, BIDIRECTIONAL
    ip_address             VARCHAR(50),
    rtsp_stream_url        VARCHAR(255),
    relay_unlock_ms        INT           NOT NULL DEFAULT 3000,
    status                 VARCHAR(50)   NOT NULL DEFAULT 'ONLINE', -- ONLINE, OFFLINE, MAINTENANCE, LOCKDOWN
    last_heartbeat         TIMESTAMP,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_turnstile_comm ON manacommunity.biometric_turnstiles(community_id);

CREATE TABLE IF NOT EXISTS manacommunity.biometric_user_enrollments (
    id                     BIGSERIAL PRIMARY KEY,
    community_id           BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    user_id                BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    person_type            VARCHAR(50)   NOT NULL, -- RESIDENT, DOMESTIC_STAFF, VENDOR_WORKER, SECURITY_GUARD
    person_name            VARCHAR(100)  NOT NULL,
    unit_number            VARCHAR(50),
    face_embedding_hash    VARCHAR(255)  NOT NULL, -- Quantized 512-D vector hash / token
    face_feature_version   VARCHAR(50)   NOT NULL DEFAULT 'FaceNet-v2',
    enrollment_status      VARCHAR(50)   NOT NULL DEFAULT 'ENROLLED', -- ENROLLED, PENDING_PHOTO, REVOKED, EXPIRED
    time_window_start      VARCHAR(10)   DEFAULT '06:00',
    time_window_end        VARCHAR(10)   DEFAULT '22:00',
    allowed_days           VARCHAR(50)   DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN',
    expiration_date        TIMESTAMP,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_biometric UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_bio_enroll_comm ON manacommunity.biometric_user_enrollments(community_id);
CREATE INDEX IF NOT EXISTS idx_bio_enroll_hash ON manacommunity.biometric_user_enrollments(face_embedding_hash);

CREATE TABLE IF NOT EXISTS manacommunity.biometric_access_logs (
    id                     BIGSERIAL PRIMARY KEY,
    turnstile_id           BIGINT        NOT NULL REFERENCES manacommunity.biometric_turnstiles(id),
    community_id           BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    user_id                BIGINT,
    person_type            VARCHAR(50)   NOT NULL,
    person_name            VARCHAR(100)  NOT NULL,
    unit_number            VARCHAR(50),
    confidence_score       NUMERIC(6,4)  NOT NULL,
    access_decision        VARCHAR(50)   NOT NULL, -- GRANTED_OPEN, DENIED_UNENROLLED, DENIED_OUTSIDE_HOURS, DENIED_REVOKED, DENIED_LOCKDOWN
    failure_reason         TEXT,
    snapshot_url           VARCHAR(255),
    timestamp              TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bio_log_comm ON manacommunity.biometric_access_logs(community_id);
CREATE INDEX IF NOT EXISTS idx_bio_log_time ON manacommunity.biometric_access_logs(timestamp);
