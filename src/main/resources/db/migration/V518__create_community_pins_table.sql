-- ============================================================================
-- V518: Create community_pins table for ephemeral hyperlocal micro-broadcasts
-- ============================================================================

CREATE TABLE IF NOT EXISTS manacommunity.community_pins (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    author_name VARCHAR(120) NOT NULL,
    flat_number VARCHAR(30),
    category VARCHAR(40) NOT NULL,
    content VARCHAR(280) NOT NULL,
    image_url VARCHAR(500),
    external_link VARCHAR(500),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP WITH TIME ZONE,
    likes_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_community_pin_community FOREIGN KEY (community_id)
        REFERENCES manacommunity.community (id) ON DELETE CASCADE,
    CONSTRAINT fk_community_pin_user FOREIGN KEY (user_id)
        REFERENCES manacommunity.app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pins_community_active
    ON manacommunity.community_pins (community_id, expires_at, is_closed);

CREATE INDEX IF NOT EXISTS idx_pins_community_created
    ON manacommunity.community_pins (community_id, created_at DESC);
