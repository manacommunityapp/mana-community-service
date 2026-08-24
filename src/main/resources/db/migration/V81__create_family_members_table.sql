-- V81: Create family_members table for resident profile family directory

CREATE TABLE IF NOT EXISTS manacommunity.family_members (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT REFERENCES manacommunity.app_users(id) ON DELETE CASCADE,
    community_id      BIGINT REFERENCES manacommunity.communities(id) ON DELETE SET NULL,
    name              VARCHAR(150) NOT NULL,
    relation          VARCHAR(60),
    age               INTEGER,
    gender            VARCHAR(20),
    dob               VARCHAR(50),
    phone             VARCHAR(50),
    email             VARCHAR(100),
    blood_group       VARCHAR(20),
    gothram           VARCHAR(100),
    emergency_contact BOOLEAN DEFAULT FALSE,
    is_devotee        BOOLEAN DEFAULT TRUE,
    avatar            VARCHAR(20),
    notes             TEXT,
    status            VARCHAR(30) DEFAULT 'ACTIVE',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fm_user_id ON manacommunity.family_members (user_id);
CREATE INDEX IF NOT EXISTS idx_fm_community_id ON manacommunity.family_members (community_id);
