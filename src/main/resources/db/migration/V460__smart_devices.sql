-- Registry of IoT/smart devices installed in the community
CREATE TABLE smart_devices (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT NOT NULL,
    device_name      VARCHAR(200) NOT NULL,
    device_type      VARCHAR(50) NOT NULL,
    -- BOOM_BARRIER, DOOR_LOCK, SMART_INTERCOM, CCTV, EV_CHARGER, LIFT_CONTROLLER
    location         VARCHAR(200) NOT NULL,
    vendor           VARCHAR(100),
    model            VARCHAR(100),
    api_endpoint     VARCHAR(500),
    api_key_ref      VARCHAR(200),  -- references env secret, never the key value
    firmware_version VARCHAR(50),
    is_online        BOOLEAN NOT NULL DEFAULT TRUE,
    last_heartbeat   TIMESTAMPTZ,
    accessible_by    VARCHAR(50) NOT NULL DEFAULT 'FM_ONLY',
    -- FM_ONLY, RESIDENT_OWN, ALL_RESIDENTS, SECURITY_STAFF
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Append-only audit of every control action
CREATE TABLE device_control_log (
    id              BIGSERIAL PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES smart_devices(id),
    community_id    BIGINT NOT NULL,
    action          VARCHAR(50) NOT NULL,
    -- UNLOCK, LOCK, OPEN, CLOSE, RESTART, STATUS_CHECK
    actor_id        BIGINT NOT NULL,
    actor_role      VARCHAR(50) NOT NULL,
    -- RESIDENT, SECURITY, FM, ADMIN, SYSTEM, VISITOR_OTP
    request_source  VARCHAR(50),
    -- MOBILE_APP, WEB, INTERCOM, SCHEDULE, VISITOR_QR
    result          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING, SUCCESS, FAILED, TIMEOUT, DENIED
    error_message   TEXT,
    duration_ms     INT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- OTP-based one-time visitor access
CREATE TABLE device_visitor_otps (
    id           BIGSERIAL PRIMARY KEY,
    device_id    BIGINT NOT NULL REFERENCES smart_devices(id),
    community_id BIGINT NOT NULL,
    issued_by    BIGINT NOT NULL,
    otp_code     VARCHAR(10) NOT NULL,
    visitor_name VARCHAR(200),
    valid_from   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until  TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (device_id, otp_code, valid_until)
);

CREATE INDEX idx_smart_devices_community ON smart_devices(community_id, device_type);
CREATE INDEX idx_device_control_log_device ON device_control_log(device_id, occurred_at DESC);
CREATE INDEX idx_device_control_log_actor ON device_control_log(actor_id, occurred_at DESC);
CREATE INDEX idx_visitor_otps_lookup ON device_visitor_otps(device_id, otp_code)
    WHERE revoked = FALSE AND used_at IS NULL;
