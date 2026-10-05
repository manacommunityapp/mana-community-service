-- V163__mana_intelligence_graph.sql
-- Schema for Mana Intelligence Layer, Graph Discoverability & Server-Enforced Privacy

CREATE TABLE IF NOT EXISTS community_graph_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    community_id BIGINT REFERENCES community(id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    tower VARCHAR(30),
    flat_no VARCHAR(20),
    bio TEXT,
    professions_json TEXT DEFAULT '[]',
    skills_json TEXT DEFAULT '[]',
    interests_json TEXT DEFAULT '[]',
    sports_json TEXT DEFAULT '[]',
    availability_hours VARCHAR(100),
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC', -- PUBLIC, NEIGHBORS, PRIVATE
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cgp_user ON community_graph_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_cgp_community ON community_graph_profile(community_id);
CREATE INDEX IF NOT EXISTS idx_cgp_visibility ON community_graph_profile(visibility);
CREATE INDEX IF NOT EXISTS idx_cgp_tower ON community_graph_profile(tower);

CREATE TABLE IF NOT EXISTS community_graph_edge (
    id BIGSERIAL PRIMARY KEY,
    source_user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    target_user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    edge_type VARCHAR(50) NOT NULL, -- NEIGHBOR, CO_PARTICIPANT, SPORTS_BUDDY, TRUSTED_CONNECTION
    weight INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_graph_edge UNIQUE (source_user_id, target_user_id, edge_type)
);

CREATE INDEX IF NOT EXISTS idx_cge_source ON community_graph_edge(source_user_id);
CREATE INDEX IF NOT EXISTS idx_cge_target ON community_graph_edge(target_user_id);