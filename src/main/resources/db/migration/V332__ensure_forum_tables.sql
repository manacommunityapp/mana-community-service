-- V332: Ensure community forum tables exist
-- Tables: forum_posts, forum_comments (from V230 which was skipped due to historical Flyway duplicate version collision)

CREATE TABLE IF NOT EXISTS manacommunity.forum_posts (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200)  NOT NULL,
    content         TEXT          NOT NULL,
    category        VARCHAR(50),
    author_id       BIGINT        NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    community_id    BIGINT        NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    comment_count   INT           NOT NULL DEFAULT 0,
    like_count      INT           NOT NULL DEFAULT 0,
    is_pinned       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_closed       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.forum_comments (
    id          BIGSERIAL PRIMARY KEY,
    content     TEXT      NOT NULL,
    author_id   BIGINT    NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    post_id     BIGINT    NOT NULL REFERENCES manacommunity.forum_posts(id) ON DELETE CASCADE,
    like_count  INT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Ensure all columns exist even if tables were partially created
ALTER TABLE manacommunity.forum_posts
    ADD COLUMN IF NOT EXISTS title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS content TEXT,
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS author_id BIGINT,
    ADD COLUMN IF NOT EXISTS community_id BIGINT,
    ADD COLUMN IF NOT EXISTS comment_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS like_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_closed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

ALTER TABLE manacommunity.forum_comments
    ADD COLUMN IF NOT EXISTS content TEXT,
    ADD COLUMN IF NOT EXISTS author_id BIGINT,
    ADD COLUMN IF NOT EXISTS post_id BIGINT,
    ADD COLUMN IF NOT EXISTS like_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

-- Create supporting indexes
CREATE INDEX IF NOT EXISTS idx_forum_post_community ON manacommunity.forum_posts(community_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_author    ON manacommunity.forum_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_created   ON manacommunity.forum_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_forum_post_category  ON manacommunity.forum_posts(community_id, category);
CREATE INDEX IF NOT EXISTS idx_forum_comment_post   ON manacommunity.forum_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_forum_comment_author ON manacommunity.forum_comments(author_id);
