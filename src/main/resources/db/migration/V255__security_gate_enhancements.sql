-- Security / Gate enhancements: vehicle blacklist, guard SOS, guard diary,
-- parcel room, staff access rules, staff leave, temp QR pass, visitor vehicle pass,
-- access cards, visitor blacklist, vendor tracking, maintenance schedule

-- ── Vehicle blacklist ─────────────────────────────────────────────────────────
CREATE TABLE vehicle_blacklist (
    id                     BIGSERIAL    PRIMARY KEY,
    community_id           BIGINT       NOT NULL,
    plate_number           VARCHAR(30)  NOT NULL,
    reason                 TEXT,
    blacklisted_by_user_id BIGINT,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, plate_number)
);
CREATE INDEX idx_vehicle_blacklist_community ON vehicle_blacklist(community_id);
CREATE INDEX idx_vehicle_blacklist_plate     ON vehicle_blacklist(plate_number);

-- ── Guard SOS / Panic button ──────────────────────────────────────────────────
CREATE TABLE guard_sos (
    id                BIGSERIAL         PRIMARY KEY,
    community_id      BIGINT            NOT NULL,
    guard_user_id     BIGINT            NOT NULL,
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    message           TEXT,
    status            VARCHAR(20)       NOT NULL DEFAULT 'ACTIVE',
    acknowledged_by   BIGINT,
    acknowledged_at   TIMESTAMP,
    created_at        TIMESTAMP         NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_guard_sos_community ON guard_sos(community_id);
CREATE INDEX idx_guard_sos_status    ON guard_sos(status);

-- ── Guard daily log / diary ───────────────────────────────────────────────────
CREATE TABLE guard_daily_log (
    id            BIGSERIAL   PRIMARY KEY,
    community_id  BIGINT      NOT NULL,
    guard_user_id BIGINT      NOT NULL,
    log_date      DATE        NOT NULL,
    shift_type    VARCHAR(20) NOT NULL DEFAULT 'MORNING',
    entry         TEXT        NOT NULL,
    category      VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_guard_daily_log_community ON guard_daily_log(community_id);
CREATE INDEX idx_guard_daily_log_date      ON guard_daily_log(log_date);

-- ── Parcel room / delivery OTP ────────────────────────────────────────────────
CREATE TABLE parcel_record (
    id                   BIGSERIAL    PRIMARY KEY,
    community_id         BIGINT       NOT NULL,
    flat_no              VARCHAR(30)  NOT NULL,
    resident_user_id     BIGINT,
    courier_name         VARCHAR(100),
    tracking_number      VARCHAR(100),
    received_by_guard_id BIGINT,
    photo_url            VARCHAR(500),
    status               VARCHAR(20)  NOT NULL DEFAULT 'AWAITING_PICKUP',
    collection_otp       VARCHAR(10),
    otp_expires_at       TIMESTAMP,
    collected_at         TIMESTAMP,
    collected_by_user_id BIGINT,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_parcel_record_community ON parcel_record(community_id);
CREATE INDEX idx_parcel_record_flat      ON parcel_record(flat_no);

-- ── Staff time-window access rules ────────────────────────────────────────────
CREATE TABLE staff_access_rule (
    id            BIGSERIAL   PRIMARY KEY,
    community_id  BIGINT      NOT NULL,
    staff_id      BIGINT      NOT NULL,
    day_of_week   VARCHAR(10) NOT NULL,
    allowed_from  TIME        NOT NULL,
    allowed_until TIME        NOT NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (staff_id, day_of_week)
);
CREATE INDEX idx_staff_access_rule_staff ON staff_access_rule(staff_id);

-- ── Staff leave ───────────────────────────────────────────────────────────────
CREATE TABLE staff_leave (
    id               BIGSERIAL   PRIMARY KEY,
    community_id     BIGINT      NOT NULL,
    staff_id         BIGINT      NOT NULL,
    resident_user_id BIGINT      NOT NULL,
    leave_date       DATE        NOT NULL,
    reason           TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (staff_id, leave_date)
);
CREATE INDEX idx_staff_leave_staff ON staff_leave(staff_id);

-- ── Temporary QR day-pass ─────────────────────────────────────────────────────
CREATE TABLE temporary_access_token (
    id                   BIGSERIAL   PRIMARY KEY,
    community_id         BIGINT      NOT NULL,
    created_by_user_id   BIGINT      NOT NULL,
    token_code           VARCHAR(64) NOT NULL UNIQUE,
    purpose              TEXT,
    visitor_name         VARCHAR(100),
    valid_from           TIMESTAMP   NOT NULL,
    valid_until          TIMESTAMP   NOT NULL,
    max_uses             INT         NOT NULL DEFAULT 1,
    use_count            INT         NOT NULL DEFAULT 0,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_temp_token_community ON temporary_access_token(community_id);
CREATE INDEX idx_temp_token_code      ON temporary_access_token(token_code);

-- ── Visitor vehicle day-pass ──────────────────────────────────────────────────
CREATE TABLE visitor_vehicle_pass (
    id               BIGSERIAL   PRIMARY KEY,
    community_id     BIGINT      NOT NULL,
    resident_user_id BIGINT      NOT NULL,
    plate_number     VARCHAR(30) NOT NULL,
    visitor_name     VARCHAR(100),
    valid_date       DATE        NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_visitor_vehicle_pass_community ON visitor_vehicle_pass(community_id);
CREATE INDEX idx_visitor_vehicle_pass_plate     ON visitor_vehicle_pass(plate_number);

-- ── RFID / access card management ────────────────────────────────────────────
CREATE TABLE access_card (
    id                  BIGSERIAL   PRIMARY KEY,
    community_id        BIGINT      NOT NULL,
    user_id             BIGINT      NOT NULL,
    flat_no             VARCHAR(30),
    card_number         VARCHAR(60) NOT NULL,
    card_type           VARCHAR(20) NOT NULL DEFAULT 'RFID',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_date         DATE,
    expiry_date         DATE,
    deactivated_reason  TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, card_number)
);
CREATE INDEX idx_access_card_community ON access_card(community_id);
CREATE INDEX idx_access_card_user      ON access_card(user_id);

-- ── Visitor photo capture: add columns to visitor_pass ───────────────────────
ALTER TABLE visitor_pass
    ADD COLUMN IF NOT EXISTS photo_captured_at TIMESTAMP NULL;
-- (visitor_photo column already exists on visitor_pass)

-- ── Visitor blacklist ─────────────────────────────────────────────────────────
CREATE TABLE visitor_blacklist (
    id                     BIGSERIAL   PRIMARY KEY,
    community_id           BIGINT      NOT NULL,
    name                   VARCHAR(100),
    phone                  VARCHAR(20),
    aadhaar_last4          VARCHAR(4),
    reason                 TEXT        NOT NULL,
    blacklisted_by_user_id BIGINT,
    is_active              BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_visitor_blacklist_community ON visitor_blacklist(community_id);
CREATE INDEX idx_visitor_blacklist_phone     ON visitor_blacklist(phone);

-- ── Vendor / technician live tracking ────────────────────────────────────────
CREATE TABLE vendor_tracking_session (
    id           BIGSERIAL         PRIMARY KEY,
    community_id BIGINT            NOT NULL,
    booking_id   BIGINT,
    vendor_id    BIGINT            NOT NULL,
    latitude     DOUBLE PRECISION  NOT NULL,
    longitude    DOUBLE PRECISION  NOT NULL,
    accuracy     DOUBLE PRECISION,
    recorded_at  TIMESTAMP         NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vendor_tracking_community ON vendor_tracking_session(community_id);
CREATE INDEX idx_vendor_tracking_booking   ON vendor_tracking_session(booking_id);
CREATE INDEX idx_vendor_tracking_recorded  ON vendor_tracking_session(recorded_at);

-- ── Recurring maintenance schedule ───────────────────────────────────────────
CREATE TABLE maintenance_schedule (
    id                 BIGSERIAL   PRIMARY KEY,
    community_id       BIGINT      NOT NULL,
    title              VARCHAR(200) NOT NULL,
    category           VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    recurrence_type    VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    next_due_date      DATE         NOT NULL,
    last_done_date     DATE,
    assigned_vendor_id BIGINT,
    notes              TEXT,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_maintenance_schedule_community ON maintenance_schedule(community_id);
CREATE INDEX idx_maintenance_schedule_due       ON maintenance_schedule(next_due_date);
