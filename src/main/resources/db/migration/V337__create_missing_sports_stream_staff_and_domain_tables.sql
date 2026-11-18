-- ============================================================================
-- V337: Create missing sports stream, staff, transaction core, engagement,
--       and worker domain tables.
-- Idempotent and non-destructive for all existing database objects.
-- ============================================================================

-- 1. sports_match_stream
CREATE TABLE IF NOT EXISTS manacommunity.sports_match_stream (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    title VARCHAR(300),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    viewer_peak INT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sports_stream_match ON manacommunity.sports_match_stream(match_id);

-- 2. sports_match_stream_camera
CREATE TABLE IF NOT EXISTS manacommunity.sports_match_stream_camera (
    id BIGSERIAL PRIMARY KEY,
    stream_id BIGINT NOT NULL REFERENCES manacommunity.sports_match_stream(id) ON DELETE CASCADE,
    label VARCHAR(100) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    embed_url VARCHAR(2048) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_stream_camera_stream ON manacommunity.sports_match_stream_camera(stream_id);

-- 3. sports_match_stream_message
CREATE TABLE IF NOT EXISTS manacommunity.sports_match_stream_message (
    id BIGSERIAL PRIMARY KEY,
    stream_id BIGINT NOT NULL REFERENCES manacommunity.sports_match_stream(id) ON DELETE CASCADE,
    author_name VARCHAR(100) NOT NULL,
    text VARCHAR(500) NOT NULL,
    is_commentary BOOLEAN NOT NULL DEFAULT FALSE,
    posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_stream_msg_stream ON manacommunity.sports_match_stream_message(stream_id);

-- 4. sports_event_dispute_committee
CREATE TABLE IF NOT EXISTS manacommunity.sports_event_dispute_committee (
    sports_event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (sports_event_id, user_id)
);

-- 5. staff_attendance
CREATE TABLE IF NOT EXISTS manacommunity.staff_attendance (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_MARKED',
    check_in_time TIME,
    check_out_time TIME,
    check_in_gate VARCHAR(100),
    check_out_gate VARCHAR(100),
    marked_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_staff_attendance_staff ON manacommunity.staff_attendance(staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_attendance_date ON manacommunity.staff_attendance(date);
CREATE INDEX IF NOT EXISTS idx_staff_attendance_status ON manacommunity.staff_attendance(status);

-- 6. staff_job_post
CREATE TABLE IF NOT EXISTS manacommunity.staff_job_post (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    posted_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    description VARCHAR(1000),
    salary_range VARCHAR(50),
    shift_preference VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    applicant_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_staff_job_post_community ON manacommunity.staff_job_post(community_id);
CREATE INDEX IF NOT EXISTS idx_staff_job_post_status ON manacommunity.staff_job_post(status);
CREATE INDEX IF NOT EXISTS idx_staff_job_post_role ON manacommunity.staff_job_post(role);

-- 7. staff_job_post_requirements
CREATE TABLE IF NOT EXISTS manacommunity.staff_job_post_requirements (
    job_post_id BIGINT NOT NULL REFERENCES manacommunity.staff_job_post(id) ON DELETE CASCADE,
    requirement VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_staff_job_post_reqs_post ON manacommunity.staff_job_post_requirements(job_post_id);

-- 8. transaction_escrow
CREATE TABLE IF NOT EXISTS manacommunity.transaction_escrow (
    id BIGSERIAL PRIMARY KEY,
    escrow_id VARCHAR(64) NOT NULL UNIQUE,
    intent_id VARCHAR(64) NOT NULL,
    held_amount NUMERIC(14,2) NOT NULL,
    released_amount NUMERIC(14,2) DEFAULT 0.00,
    refunded_amount NUMERIC(14,2) DEFAULT 0.00,
    release_condition VARCHAR(32) NOT NULL,
    verification_code VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'HELD',
    payee_id BIGINT,
    payee_type VARCHAR(32),
    verified_by VARCHAR(100),
    release_notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_escrow_id ON manacommunity.transaction_escrow(escrow_id);
CREATE INDEX IF NOT EXISTS idx_escrow_intent ON manacommunity.transaction_escrow(intent_id);
CREATE INDEX IF NOT EXISTS idx_escrow_status ON manacommunity.transaction_escrow(status);
CREATE INDEX IF NOT EXISTS idx_escrow_payee ON manacommunity.transaction_escrow(payee_id, payee_type);

-- 9. transaction_reconciliation_log
CREATE TABLE IF NOT EXISTS manacommunity.transaction_reconciliation_log (
    id BIGSERIAL PRIMARY KEY,
    recon_id VARCHAR(64) NOT NULL UNIQUE,
    recon_date DATE NOT NULL,
    community_id BIGINT,
    total_transactions INT DEFAULT 0,
    matched_count INT DEFAULT 0,
    mismatched_count INT DEFAULT 0,
    auto_resolved_count INT DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    discrepancies_json JSONB,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_recon_id ON manacommunity.transaction_reconciliation_log(recon_id);
CREATE INDEX IF NOT EXISTS idx_recon_date ON manacommunity.transaction_reconciliation_log(recon_date);

-- 10. transaction_settlement
CREATE TABLE IF NOT EXISTS manacommunity.transaction_settlement (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(64) NOT NULL UNIQUE,
    batch_id VARCHAR(64),
    intent_id VARCHAR(64) NOT NULL,
    payee_id BIGINT NOT NULL,
    payee_type VARCHAR(32) NOT NULL,
    payee_account VARCHAR(64),
    gross_amount NUMERIC(14,2) NOT NULL,
    commission_amount NUMERIC(14,2) DEFAULT 0.00,
    tds_amount NUMERIC(14,2) DEFAULT 0.00,
    net_payout_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payout_reference VARCHAR(100),
    journal_entry_number VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_settlement_id ON manacommunity.transaction_settlement(settlement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_batch ON manacommunity.transaction_settlement(batch_id);
CREATE INDEX IF NOT EXISTS idx_settlement_payee ON manacommunity.transaction_settlement(payee_id, payee_type);
CREATE INDEX IF NOT EXISTS idx_settlement_intent ON manacommunity.transaction_settlement(intent_id);

-- 11. trending_topic
CREATE TABLE IF NOT EXISTS manacommunity.trending_topic (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    topic VARCHAR(200) NOT NULL,
    topic_type VARCHAR(30) NOT NULL DEFAULT 'HASHTAG',
    post_count INT NOT NULL DEFAULT 0,
    engagement_count INT NOT NULL DEFAULT 0,
    score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    time_window_hours INT NOT NULL DEFAULT 24,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_trending_community ON manacommunity.trending_topic(community_id);
CREATE INDEX IF NOT EXISTS idx_trending_score ON manacommunity.trending_topic(score DESC);

-- 12. user_badge
CREATE TABLE IF NOT EXISTS manacommunity.user_badge (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    badge_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(10),
    points_value INT NOT NULL DEFAULT 0,
    earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_badge_user ON manacommunity.user_badge(user_id);
CREATE INDEX IF NOT EXISTS idx_user_badge_community ON manacommunity.user_badge(community_id);

-- 13. user_engagement_score
CREATE TABLE IF NOT EXISTS manacommunity.user_engagement_score (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    posts_count INT NOT NULL DEFAULT 0,
    comments_count INT NOT NULL DEFAULT 0,
    reactions_given INT NOT NULL DEFAULT 0,
    reactions_received INT NOT NULL DEFAULT 0,
    groups_joined INT NOT NULL DEFAULT 0,
    events_attended INT NOT NULL DEFAULT 0,
    helpful_count INT NOT NULL DEFAULT 0,
    volunteer_points INT NOT NULL DEFAULT 0,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    rank_position INT,
    last_activity_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_engagement_user_community UNIQUE (user_id, community_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_engagement_user_comm ON manacommunity.user_engagement_score(user_id, community_id);

-- 14. user_hub_usage
CREATE TABLE IF NOT EXISTS manacommunity.user_hub_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    hub_id VARCHAR(50) NOT NULL,
    hub_label VARCHAR(100),
    click_count INT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_hub UNIQUE (user_id, hub_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_hub ON manacommunity.user_hub_usage(user_id, hub_id);

-- 15. user_location_consent
CREATE TABLE IF NOT EXISTS manacommunity.user_location_consent (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    is_granted BOOLEAN NOT NULL DEFAULT FALSE,
    consent_version VARCHAR(20) NOT NULL DEFAULT 'DPDP_2023_V1',
    client_ip VARCHAR(45),
    granted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_loc_consent_user ON manacommunity.user_location_consent(user_id);
CREATE INDEX IF NOT EXISTS idx_loc_consent_purpose ON manacommunity.user_location_consent(user_id, purpose);

-- 16. user_profile
CREATE TABLE IF NOT EXISTS manacommunity.user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    bio VARCHAR(1000),
    cover_pic_url VARCHAR(255),
    skills VARCHAR(1000),
    posts_count INT DEFAULT 0,
    connections_count INT DEFAULT 0,
    events_attended_count INT DEFAULT 0,
    items_sold_count INT DEFAULT 0,
    jobs_posted_count INT DEFAULT 0,
    sports_played_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_profile_user ON manacommunity.user_profile(user_id);

-- 17. visitor_audit_log
CREATE TABLE IF NOT EXISTS manacommunity.visitor_audit_log (
    id BIGSERIAL PRIMARY KEY,
    visitor_pass_id BIGINT,
    visitor_name VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100),
    details VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_visitor_pass_id ON manacommunity.visitor_audit_log(visitor_pass_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON manacommunity.visitor_audit_log(timestamp);

-- 18. worker_availability_slots
CREATE TABLE IF NOT EXISTS manacommunity.worker_availability_slots (
    id VARCHAR(64) PRIMARY KEY,
    worker_id VARCHAR(64) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_booked BOOLEAN DEFAULT FALSE,
    max_concurrent_bookings INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_worker_avail_worker ON manacommunity.worker_availability_slots(worker_id);

-- 19. worker_flat_assignments
CREATE TABLE IF NOT EXISTS manacommunity.worker_flat_assignments (
    id VARCHAR(64) PRIMARY KEY,
    worker_id VARCHAR(64) NOT NULL,
    community_id VARCHAR(64) NOT NULL,
    flat_id VARCHAR(64),
    flat_number VARCHAR(50) NOT NULL,
    tower VARCHAR(50),
    resident_user_id VARCHAR(64),
    resident_name VARCHAR(150),
    service_category_id VARCHAR(64),
    schedule_summary VARCHAR(255),
    start_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_worker_flat_worker ON manacommunity.worker_flat_assignments(worker_id);
CREATE INDEX IF NOT EXISTS idx_worker_flat_community ON manacommunity.worker_flat_assignments(community_id);

-- 20. worker_gate_pass_records
CREATE TABLE IF NOT EXISTS manacommunity.worker_gate_pass_records (
    id VARCHAR(64) PRIMARY KEY,
    worker_id VARCHAR(64) NOT NULL,
    community_id VARCHAR(64) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    qr_token_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_worker_gp_worker ON manacommunity.worker_gate_pass_records(worker_id);
CREATE INDEX IF NOT EXISTS idx_worker_gp_community ON manacommunity.worker_gate_pass_records(community_id);

-- 21. worker_service_skills
CREATE TABLE IF NOT EXISTS manacommunity.worker_service_skills (
    id VARCHAR(64) PRIMARY KEY,
    worker_id VARCHAR(64) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    experience_years INT DEFAULT 0,
    base_price_monthly NUMERIC(10,2),
    base_price_per_day NUMERIC(10,2),
    base_price_per_visit NUMERIC(10,2),
    base_price_per_hour NUMERIC(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_worker_skill_worker ON manacommunity.worker_service_skills(worker_id);
CREATE INDEX IF NOT EXISTS idx_worker_skill_category ON manacommunity.worker_service_skills(category_id);
