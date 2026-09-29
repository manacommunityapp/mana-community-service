-- ──────────────────────────────────────────────────────────────
-- Push Notification Module — New Tables
-- Run AFTER V4__admin_tables.sql.
-- Flyway: name this V5__push_tokens.sql
-- Manual: psql -U postgres -d manacommunity -f V5__push_tokens.sql
-- ──────────────────────────────────────────────────────────────

-- ── push_token ────────────────────────────────────────────────
-- Stores Expo push tokens (one row per device).
-- Multiple rows per user are expected (phone + tablet).
-- Deactivated = soft-delete; cleaned up by PushTokenCleanupScheduler.

CREATE TABLE IF NOT EXISTS push_token (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token       VARCHAR(200) NOT NULL,
    platform    VARCHAR(10)  NOT NULL DEFAULT 'android',  -- 'ios' | 'android'
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_push_token_value UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_push_token_user
    ON push_token (user_id);

CREATE INDEX IF NOT EXISTS idx_push_token_value
    ON push_token (token);

-- Partial index: most queries only care about active tokens
CREATE INDEX IF NOT EXISTS idx_push_token_user_active
    ON push_token (user_id)
    WHERE active = TRUE;

-- Comment for documentation
COMMENT ON TABLE push_token IS
    'Expo push notification tokens. One row per device. active=FALSE means '
    'the token was deactivated (logout or DeviceNotRegistered from Expo API). '
    'Hard-deleted after 30 days by PushTokenCleanupScheduler.';

COMMENT ON COLUMN push_token.token IS
    'Expo push token string, e.g. ExponentPushToken[xxxxxx]. '
    'Unique across the entire table — one device = one row regardless of user.';
