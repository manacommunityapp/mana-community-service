CREATE TABLE IF NOT EXISTS manacommunity.buying_group (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    tower VARCHAR(128),
    block VARCHAR(128),
    leader_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    description TEXT,
    total_saved NUMERIC(12, 2) DEFAULT 0,
    member_count INT DEFAULT 1,
    active_deals_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_buying_group_comm ON manacommunity.buying_group(community_id);