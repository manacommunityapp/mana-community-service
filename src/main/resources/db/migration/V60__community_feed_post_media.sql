-- V60: Create post_media table and supporting tables for the community feed.
-- The post, post_like, post_comment, post_reaction, post_bookmark, post_poll_vote,
-- post_hashtag, and hashtag tables were created via ddl-auto before Flyway; this
-- migration creates them idempotently for fresh environments (e.g. prod restore)
-- and adds the media_object_id column to post_media for fresh URL generation.

SET search_path = manacommunity;

-- ── post table ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    community_id       BIGINT       NOT NULL,
    group_id           BIGINT,
    content            TEXT         NOT NULL,
    title              VARCHAR(500),
    image_url          VARCHAR(2000),
    post_type          VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    visibility         VARCHAR(30)  NOT NULL DEFAULT 'COMMUNITY',
    priority           VARCHAR(30)  NOT NULL DEFAULT 'NORMAL',
    official           BOOLEAN      NOT NULL DEFAULT FALSE,
    pinned             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP,
    likes_count        INT          NOT NULL DEFAULT 0,
    comments_count     INT          NOT NULL DEFAULT 0,
    shares_count       INT          NOT NULL DEFAULT 0,
    bookmarks_count    INT          NOT NULL DEFAULT 0,
    views_count        INT          NOT NULL DEFAULT 0,
    price              NUMERIC(15,2),
    location           VARCHAR(500),
    event_date         TIMESTAMP,
    event_end_date     TIMESTAMP,
    event_venue        VARCHAR(500),
    event_id           BIGINT,
    poll_question      VARCHAR(500),
    poll_options       TEXT,
    poll_end_date      TIMESTAMP,
    poll_anonymous     BOOLEAN      NOT NULL DEFAULT FALSE,
    hashtags           TEXT,
    mentions           TEXT,
    link_url           VARCHAR(2000),
    link_title         VARCHAR(500),
    link_description   TEXT,
    link_image         VARCHAR(2000),
    moderation_status  VARCHAR(30),
    notify             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_post_community ON post (community_id);
CREATE INDEX IF NOT EXISTS idx_post_user      ON post (user_id);
CREATE INDEX IF NOT EXISTS idx_post_type      ON post (community_id, post_type);

-- ── post_media table ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_media (
    id                     BIGSERIAL    PRIMARY KEY,
    post_id                BIGINT       NOT NULL REFERENCES post(id),
    media_url              VARCHAR(1000) NOT NULL,
    media_type             VARCHAR(20)  NOT NULL,
    thumbnail_url          VARCHAR(1000),
    alt_text               VARCHAR(500),
    sort_order             INT          NOT NULL DEFAULT 0,
    file_size              BIGINT,
    width                  INT,
    height                 INT,
    duration               INT,
    media_object_id        UUID,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_post_media_post ON post_media (post_id);

-- Add media_object_id if it doesn't exist yet (idempotent for dev envs where
-- ddl-auto=update already created the table without this column).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'manacommunity'
          AND table_name   = 'post_media'
          AND column_name  = 'media_object_id'
    ) THEN
        ALTER TABLE post_media ADD COLUMN media_object_id UUID;
    END IF;
END;
$$;

-- ── post_like ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_like (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES post(id),
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

-- ── post_reaction ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_reaction (
    id            BIGSERIAL PRIMARY KEY,
    post_id       BIGINT      NOT NULL REFERENCES post(id),
    user_id       BIGINT      NOT NULL,
    reaction_type VARCHAR(30) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

-- ── post_comment ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_comment (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT    NOT NULL REFERENCES post(id),
    user_id         BIGINT    NOT NULL,
    parent_id       BIGINT    REFERENCES post_comment(id),
    content         TEXT      NOT NULL,
    pinned          BOOLEAN   NOT NULL DEFAULT FALSE,
    accepted_answer BOOLEAN   NOT NULL DEFAULT FALSE,
    deleted         BOOLEAN   NOT NULL DEFAULT FALSE,
    likes_count     INT       NOT NULL DEFAULT 0,
    replies_count   INT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_post_comment_post ON post_comment (post_id);

-- ── post_bookmark ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_bookmark (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES post(id),
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

-- ── post_poll_vote ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_poll_vote (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT      NOT NULL REFERENCES post(id),
    user_id         BIGINT      NOT NULL,
    selected_option VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

-- ── hashtag ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hashtag (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    community_id BIGINT       NOT NULL,
    post_count   INT          NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (name, community_id)
);

-- ── post_hashtag ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post_hashtag (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES post(id),
    hashtag_id BIGINT NOT NULL REFERENCES hashtag(id),
    UNIQUE (post_id, hashtag_id)
);
