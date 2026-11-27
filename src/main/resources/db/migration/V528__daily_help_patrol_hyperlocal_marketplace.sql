-- Daily Help Cross-Society Attendance, Guard Patrol Security, and Hyperlocal S2 Marketplace

-- 1. Daily Help Cross-Society & Private Flat Contracts
CREATE TABLE IF NOT EXISTS manacommunity.global_staff_profile (
    id                   BIGSERIAL PRIMARY KEY,
    staff_uuid           VARCHAR(64) NOT NULL UNIQUE,
    phone_hash           VARCHAR(64) NOT NULL UNIQUE,
    masked_phone         VARCHAR(20) NOT NULL,
    display_name         VARCHAR(120) NOT NULL,
    primary_category     VARCHAR(30) NOT NULL,
    aggregate_rating     NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    rating_sample_size   INT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.society_staff_affiliation (
    id                   BIGSERIAL PRIMARY KEY,
    staff_id             BIGINT NOT NULL REFERENCES manacommunity.global_staff_profile(id) ON DELETE RESTRICT,
    community_id         BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    local_badge_id       VARCHAR(32) NOT NULL,
    current_gate_state   VARCHAR(10) NOT NULL DEFAULT 'OUT',
    last_gate_event_at   TIMESTAMP,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_society_staff UNIQUE (community_id, staff_id)
);

CREATE INDEX IF NOT EXISTS idx_society_staff_comm ON manacommunity.society_staff_affiliation(community_id, status);

CREATE TABLE IF NOT EXISTS manacommunity.flat_staff_contract (
    id                          BIGSERIAL PRIMARY KEY,
    affiliation_id              BIGINT NOT NULL REFERENCES manacommunity.society_staff_affiliation(id) ON DELETE RESTRICT,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    flat_id                     BIGINT NOT NULL,
    resident_id                 BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    service_type                VARCHAR(30) NOT NULL,
    days_of_week_mask           INT NOT NULL DEFAULT 127,
    scheduled_start_time        TIME NOT NULL,
    scheduled_end_time          TIME NOT NULL,
    monthly_stipend             NUMERIC(10, 2) NOT NULL,
    leave_allowance_per_month   INT NOT NULL DEFAULT 2,
    deduction_policy            VARCHAR(30) NOT NULL DEFAULT 'WORKING_DAYS_PRO_RATA',
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_flat_staff_contract UNIQUE (flat_id, affiliation_id, service_type)
);

CREATE INDEX IF NOT EXISTS idx_flat_staff_contract_lookup ON manacommunity.flat_staff_contract(community_id, flat_id, is_active);

CREATE TABLE IF NOT EXISTS manacommunity.flat_attendance_session (
    id                          BIGSERIAL PRIMARY KEY,
    contract_id                 BIGINT NOT NULL REFERENCES manacommunity.flat_staff_contract(id) ON DELETE CASCADE,
    work_date                   DATE NOT NULL,
    attendance_status           VARCHAR(20) NOT NULL,
    scheduled_minutes           INT NOT NULL,
    society_dwell_minutes       INT DEFAULT 0,
    pro_rata_multiplier         NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    resident_override_status    VARCHAR(20),
    override_reason             VARCHAR(255),
    reconciled_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_flat_attendance_session UNIQUE (contract_id, work_date)
);

CREATE INDEX IF NOT EXISTS idx_flat_attendance_contract_date ON manacommunity.flat_attendance_session(contract_id, work_date);

CREATE TABLE IF NOT EXISTS manacommunity.staff_monthly_salary_ledger (
    id                          BIGSERIAL PRIMARY KEY,
    contract_id                 BIGINT NOT NULL REFERENCES manacommunity.flat_staff_contract(id) ON DELETE CASCADE,
    billing_month               VARCHAR(7) NOT NULL,
    calendar_days               INT NOT NULL,
    scheduled_working_days      INT NOT NULL,
    present_days                INT NOT NULL,
    paid_leave_days             INT NOT NULL,
    unpaid_absence_days         INT NOT NULL,
    base_stipend                NUMERIC(10, 2) NOT NULL,
    pro_rata_deduction          NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    net_payable                 NUMERIC(10, 2) NOT NULL,
    payment_status              VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    calculated_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_staff_monthly_salary UNIQUE (contract_id, billing_month)
);

-- 2. Guard Patrols & Anti-Spoofing Verification
CREATE TABLE IF NOT EXISTS manacommunity.patrol_checkpoint_node (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    route_name                  VARCHAR(100) NOT NULL,
    sequence_order              INT NOT NULL,
    name                        VARCHAR(100) NOT NULL,
    latitude                    NUMERIC(10, 7) NOT NULL,
    longitude                   NUMERIC(10, 7) NOT NULL,
    geofence_radius_meters      INT NOT NULL DEFAULT 25,
    tag_type                    VARCHAR(20) NOT NULL DEFAULT 'NTAG_424_DNA',
    nfc_tag_uid                 VARCHAR(32),
    nfc_key_secret              VARCHAR(64),
    last_known_tap_counter      BIGINT NOT NULL DEFAULT 0,
    expected_offset_minutes     INT NOT NULL DEFAULT 0,
    allowed_grace_minutes       INT NOT NULL DEFAULT 10,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_checkpoint_route_seq UNIQUE (community_id, route_name, sequence_order)
);

CREATE TABLE IF NOT EXISTS manacommunity.patrol_scan_audit (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    checkpoint_id               BIGINT NOT NULL REFERENCES manacommunity.patrol_checkpoint_node(id) ON DELETE CASCADE,
    guard_user_id               BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    shift_id                    BIGINT,
    scanned_at_device_boot_ms   BIGINT NOT NULL,
    server_received_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    latitude                    NUMERIC(10, 7) NOT NULL,
    longitude                   NUMERIC(10, 7) NOT NULL,
    gps_accuracy_meters         NUMERIC(6, 2),
    is_mock_detected            BOOLEAN NOT NULL DEFAULT FALSE,
    nfc_counter_verified        BOOLEAN NOT NULL DEFAULT TRUE,
    ble_beacon_verified         BOOLEAN NOT NULL DEFAULT TRUE,
    kinematic_speed_kmh         NUMERIC(5, 2),
    scan_validity_status        VARCHAR(30) NOT NULL,
    rejection_reason            VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_patrol_scan_audit_comm ON manacommunity.patrol_scan_audit(community_id, shift_id, server_received_at DESC);

CREATE TABLE IF NOT EXISTS manacommunity.patrol_breach_incident (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    checkpoint_id               BIGINT NOT NULL REFERENCES manacommunity.patrol_checkpoint_node(id) ON DELETE CASCADE,
    guard_user_id               BIGINT,
    shift_id                    BIGINT,
    expected_deadline           TIMESTAMP NOT NULL,
    breach_type                 VARCHAR(30) NOT NULL,
    escalation_status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    triggered_at                TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Hyperlocal S2 Marketplace
CREATE TABLE IF NOT EXISTS manacommunity.market_cluster_listing (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    seller_user_id              BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    title                       VARCHAR(150) NOT NULL,
    description                 TEXT NOT NULL,
    category                    VARCHAR(40) NOT NULL,
    price                       NUMERIC(10, 2) NOT NULL,
    visibility_scope            VARCHAR(20) NOT NULL DEFAULT 'CLUSTER_3KM',
    latitude                    NUMERIC(10, 7) NOT NULL,
    longitude                   NUMERIC(10, 7) NOT NULL,
    s2_cell_id_l13              VARCHAR(16) NOT NULL,
    seller_trust_score          NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    is_seller_verified_resident BOOLEAN NOT NULL DEFAULT TRUE,
    moderation_status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    fraud_risk_score            NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    report_count                INT NOT NULL DEFAULT 0,
    expires_at                  TIMESTAMP NOT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_market_cluster_s2 ON manacommunity.market_cluster_listing(s2_cell_id_l13, moderation_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_market_cluster_comm_cat ON manacommunity.market_cluster_listing(community_id, category, moderation_status);
