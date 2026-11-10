-- ============================================================================
-- V275: Mana Kitty Party Feed: Posts, Announcements & Comments
-- Schema: manacommunity
-- ============================================================================

-- 1. Kitty Circle Feed Posts Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_post (
    id                 BIGSERIAL PRIMARY KEY,
    circle_id          BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    author_user_id     BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    post_type          VARCHAR(30) NOT NULL DEFAULT 'TEXT'
                       CHECK (post_type IN ('TEXT', 'ANNOUNCEMENT', 'PHOTO', 'POLL')),
    title              VARCHAR(200),
    content            TEXT NOT NULL,
    attachment_url     TEXT,
    is_pinned          BOOLEAN NOT NULL DEFAULT FALSE,
    status             VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
                       CHECK (status IN ('PUBLISHED', 'PINNED', 'ARCHIVED', 'DELETED')),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kitty_post_circle ON manacommunity.kitty_post(circle_id, is_pinned, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_kitty_post_author ON manacommunity.kitty_post(author_user_id);

-- 2. Kitty Circle Post Comments Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_post_comment (
    id                 BIGSERIAL PRIMARY KEY,
    post_id            BIGINT NOT NULL REFERENCES manacommunity.kitty_post(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    comment_text       TEXT NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kitty_comment_post ON manacommunity.kitty_post_comment(post_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_kitty_comment_user ON manacommunity.kitty_post_comment(user_id);
