-- ──────────────────────────────────────────────────────────────────────────────
-- V231: Sports Tournament Player Payment & Audit Log
--   Table: sports_tournament_player_payment
-- ──────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_tournament_player_payment (
    id                  BIGSERIAL     PRIMARY KEY,
    tournament_id       BIGINT        NOT NULL REFERENCES sports_tournament(id) ON DELETE CASCADE,
    player_key          VARCHAR(100)  NOT NULL,
    user_id             BIGINT        REFERENCES app_user(id) ON DELETE SET NULL,
    family_member_id    BIGINT        REFERENCES family_members(id) ON DELETE SET NULL,
    player_name         VARCHAR(150)  NOT NULL,
    flat_number         VARCHAR(50),
    phone               VARCHAR(50),
    email               VARCHAR(150),
    payment_status      VARCHAR(30)   NOT NULL DEFAULT 'PAID',
    amount              NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    payment_mode        VARCHAR(30)   NOT NULL DEFAULT 'UPI',
    transaction_ref     VARCHAR(100),
    notes               VARCHAR(500),
    marked_by_user_id   BIGINT        REFERENCES app_user(id) ON DELETE SET NULL,
    marked_by_user_name VARCHAR(150),
    paid_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    CONSTRAINT uq_tourn_player_payment UNIQUE (tournament_id, player_key)
);

CREATE INDEX IF NOT EXISTS idx_tourn_payment_tourn_id   ON sports_tournament_player_payment (tournament_id);
CREATE INDEX IF NOT EXISTS idx_tourn_payment_player_key ON sports_tournament_player_payment (player_key);
CREATE INDEX IF NOT EXISTS idx_tourn_payment_status     ON sports_tournament_player_payment (payment_status);
