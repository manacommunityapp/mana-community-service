-- V286: Mana Jobs
CREATE TABLE mw_job_listings (
    id               BIGSERIAL PRIMARY KEY,
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
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_job_listing_org ON mw_job_listings(organization_id);
CREATE INDEX idx_job_listing_status ON mw_job_listings(status);

CREATE TABLE mw_job_applications (
    id            BIGSERIAL PRIMARY KEY,
    job_id        BIGINT NOT NULL,
    mana_user_id  BIGINT NOT NULL,
    passport_id   BIGINT,
    cover_note    TEXT,
    status        VARCHAR(30) DEFAULT 'APPLIED',
    applied_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at  TIMESTAMP,
    employer_note VARCHAR(500),
    CONSTRAINT uq_job_application UNIQUE (job_id, mana_user_id),
    FOREIGN KEY (job_id) REFERENCES mw_job_listings(id)
);
CREATE INDEX idx_job_app_user ON mw_job_applications(mana_user_id);
CREATE INDEX idx_job_app_job ON mw_job_applications(job_id);
