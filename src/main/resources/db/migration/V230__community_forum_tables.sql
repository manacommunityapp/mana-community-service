-- V230: Community Forum — threaded discussion boards for residents

CREATE TABLE IF NOT EXISTS forum_posts (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200)  NOT NULL,
    content         TEXT          NOT NULL,
    category        VARCHAR(50),
    author_id       BIGINT        NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    community_id    BIGINT        NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    comment_count   INT           NOT NULL DEFAULT 0,
    like_count      INT           NOT NULL DEFAULT 0,
    is_pinned       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_closed       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS forum_comments (
    id          BIGSERIAL PRIMARY KEY,
    content     TEXT     NOT NULL,
    author_id   BIGINT   NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    post_id     BIGINT   NOT NULL REFERENCES forum_posts(id) ON DELETE CASCADE,
    like_count  INT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_forum_post_community ON forum_posts(community_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_author    ON forum_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_created   ON forum_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_forum_post_category  ON forum_posts(community_id, category);
CREATE INDEX IF NOT EXISTS idx_forum_comment_post   ON forum_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_forum_comment_author ON forum_comments(author_id);
