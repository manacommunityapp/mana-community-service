CREATE TABLE IF NOT EXISTS privacy_notice_acceptances (
    id BIGSERIAL PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    notice_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pna_resident FOREIGN KEY (resident_id) REFERENCES resident(id) ON DELETE CASCADE,
    CONSTRAINT fk_pna_community FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_pna_resident ON privacy_notice_acceptances(resident_id);
CREATE INDEX IF NOT EXISTS idx_pna_community ON privacy_notice_acceptances(community_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pna_resident_version ON privacy_notice_acceptances(resident_id, notice_version);
