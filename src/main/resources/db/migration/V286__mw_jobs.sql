-- V286: Mana Jobs — community job board and application pipeline

CREATE TABLE mw_job_listings (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id  BIGINT NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    employment_type  VARCHAR(30) DEFAULT 'FULL_TIME',
    location         VARCHAR(200),
    is_remote        BOOLEAN DEFAULT FALSE,
    salary_min       DECIMAL(12,2),
    salary_max       DECIMAL(12,2),
    required_skills  TEXT,
    experience_years INT,
    status           VARCHAR(20) DEFAULT 'ACTIVE',
    expires_at       DATE,
    created_by       BIGINT NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_job_listing_org (organization_id),
    INDEX idx_job_listing_status (status)
);

CREATE TABLE mw_job_applications (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id        BIGINT NOT NULL,
    mana_user_id  BIGINT NOT NULL,
    passport_id   BIGINT,
    cover_note    TEXT,
    status        VARCHAR(30) DEFAULT 'APPLIED',
    applied_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at  TIMESTAMP,
    employer_note VARCHAR(500),
    UNIQUE KEY uq_job_application (job_id, mana_user_id),
    FOREIGN KEY (job_id) REFERENCES mw_job_listings(id),
    INDEX idx_job_app_user (mana_user_id),
    INDEX idx_job_app_job (job_id)
);
