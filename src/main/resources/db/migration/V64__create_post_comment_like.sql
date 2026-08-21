-- Migration V64: Create post_comment_like table

CREATE TABLE IF NOT EXISTS post_comment_like (
    id         BIGSERIAL PRIMARY KEY,
    comment_id BIGINT    NOT NULL REFERENCES post_comment(id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_post_comment_like_comment ON post_comment_like (comment_id);
CREATE INDEX IF NOT EXISTS idx_post_comment_like_user ON post_comment_like (user_id);
