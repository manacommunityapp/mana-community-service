-- ============================================================================
-- V276: Mana Kitty Party Phase 5 Expansion: Polls & Curated Vendor Packages
-- Schema: manacommunity
-- ============================================================================

-- 1. Kitty Feed Post Poll Options Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_poll_option (
    id                 BIGSERIAL PRIMARY KEY,
    post_id            BIGINT NOT NULL REFERENCES manacommunity.kitty_post(id) ON DELETE CASCADE,
    option_text        VARCHAR(255) NOT NULL,
    sort_order         INT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kitty_poll_opt_post ON manacommunity.kitty_poll_option(post_id, sort_order ASC);

-- 2. Kitty Feed Post Poll Votes Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_poll_vote (
    id                 BIGSERIAL PRIMARY KEY,
    post_id            BIGINT NOT NULL REFERENCES manacommunity.kitty_post(id) ON DELETE CASCADE,
    option_id          BIGINT NOT NULL REFERENCES manacommunity.kitty_poll_option(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    voted_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_kitty_poll_user_post UNIQUE (post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_kitty_poll_vote_post ON manacommunity.kitty_poll_vote(post_id);
CREATE INDEX IF NOT EXISTS idx_kitty_poll_vote_opt ON manacommunity.kitty_poll_vote(option_id);
CREATE INDEX IF NOT EXISTS idx_kitty_poll_vote_user ON manacommunity.kitty_poll_vote(user_id);

-- 3. Curated Party Vendor Packages Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_vendor_package (
    id                 BIGSERIAL PRIMARY KEY,
    community_id       BIGINT NOT NULL REFERENCES manacommunity.community(id),
    vendor_id          BIGINT,
    category           VARCHAR(50) NOT NULL 
                       CHECK (category IN ('CATERING', 'CAKES_DESSERTS', 'BALLOON_DECOR', 'EMCEE_ENTERTAINMENT', 'PHOTOGRAPHY', 'OTHER')),
    package_name       VARCHAR(200) NOT NULL,
    description        TEXT,
    price_range        VARCHAR(100),
    contact_person     VARCHAR(100),
    contact_phone      VARCHAR(30),
    sample_menu        TEXT,
    rating             NUMERIC(3, 2) DEFAULT 4.80,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kitty_vendor_pkg_comm ON manacommunity.kitty_vendor_package(community_id, category);
CREATE INDEX IF NOT EXISTS idx_kitty_vendor_pkg_active ON manacommunity.kitty_vendor_package(is_active);
