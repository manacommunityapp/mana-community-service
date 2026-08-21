-- V65: Rich comment reactions table (LIKE, LOVE, CELEBRATE, HELPFUL, THANKS)
-- The existing post_comment_like table is kept for backward-compat; new rich
-- reactions go here. Unique constraint ensures one reaction per user per comment.

CREATE TABLE IF NOT EXISTS post_comment_reaction (
    id            BIGSERIAL    PRIMARY KEY,
    comment_id    BIGINT       NOT NULL REFERENCES post_comment(id) ON DELETE CASCADE,
    user_id       BIGINT       NOT NULL,
    reaction_type VARCHAR(20)  NOT NULL DEFAULT 'LIKE',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    UNIQUE (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_pcr_comment ON post_comment_reaction (comment_id);
CREATE INDEX IF NOT EXISTS idx_pcr_user    ON post_comment_reaction (user_id);