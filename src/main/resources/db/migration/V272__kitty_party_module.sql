-- ============================================================================
-- V272: Mana Kitty Party & Social Circles Module
-- Schema: manacommunity
-- ============================================================================

-- 1. Circle Master Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_circle (
    id                 BIGSERIAL PRIMARY KEY,
    community_id       BIGINT NOT NULL REFERENCES manacommunity.community(id),
    circle_name        VARCHAR(150) NOT NULL,
    description        TEXT,
    category_code      VARCHAR(50) NOT NULL DEFAULT 'KITTY',
    privacy_type       VARCHAR(30) NOT NULL DEFAULT 'PRIVATE' 
                       CHECK (privacy_type IN ('COMMUNITY_PUBLIC', 'PRIVATE', 'INVITE_ONLY')),
    approval_required  BOOLEAN NOT NULL DEFAULT TRUE,
    max_members        INTEGER DEFAULT 20 CHECK (max_members IS NULL OR max_members > 0),
    status             VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' 
                       CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    timezone           VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    created_by         BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kitty_circle_comm ON manacommunity.kitty_circle(community_id, status);
CREATE INDEX IF NOT EXISTS idx_kitty_circle_cat ON manacommunity.kitty_circle(category_code);

-- 2. Circle Membership
CREATE TABLE IF NOT EXISTS manacommunity.kitty_circle_member (
    id                 BIGSERIAL PRIMARY KEY,
    circle_id          BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    community_id       BIGINT NOT NULL REFERENCES manacommunity.community(id),
    role_code          VARCHAR(30) NOT NULL DEFAULT 'MEMBER'
                       CHECK (role_code IN ('OWNER', 'ADMIN', 'TREASURER', 'HOST', 'MEMBER')),
    membership_status  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                       CHECK (membership_status IN ('REQUESTED', 'INVITED', 'ACTIVE', 'REJECTED', 'SUSPENDED', 'LEFT', 'REMOVED')),
    joined_at          TIMESTAMP,
    approved_by        BIGINT REFERENCES manacommunity.app_user(id),
    approved_at        TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_circle_user UNIQUE (circle_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_kitty_member_usr ON manacommunity.kitty_circle_member(user_id, membership_status);
CREATE INDEX IF NOT EXISTS idx_kitty_member_crc ON manacommunity.kitty_circle_member(circle_id, membership_status);

-- 3. Invitations and Join Requests
CREATE TABLE IF NOT EXISTS manacommunity.kitty_circle_invitation (
    id                 BIGSERIAL PRIMARY KEY,
    circle_id          BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    invited_user_id    BIGINT REFERENCES manacommunity.app_user(id),
    invited_phone      VARCHAR(20),
    invite_token_hash  VARCHAR(128) NOT NULL UNIQUE,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'REVOKED')),
    expires_at         TIMESTAMP NOT NULL,
    created_by         BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.kitty_circle_join_request (
    id                 BIGSERIAL PRIMARY KEY,
    circle_id          BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    request_message    TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    reviewed_by        BIGINT REFERENCES manacommunity.app_user(id),
    reviewed_at        TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_join_request UNIQUE (circle_id, user_id)
);

-- 4. Kitty Savings Cycle (Record-Only)
CREATE TABLE IF NOT EXISTS manacommunity.kitty_cycle (
    id                  BIGSERIAL PRIMARY KEY,
    circle_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    cycle_name          VARCHAR(150) NOT NULL,
    contribution_amount NUMERIC(14,2) NOT NULL CHECK (contribution_amount > 0),
    frequency           VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'BI_WEEKLY')),
    member_count        INTEGER NOT NULL CHECK (member_count > 0),
    start_date          DATE NOT NULL,
    end_date            DATE,
    financial_mode      VARCHAR(30) NOT NULL DEFAULT 'RECORD_ONLY' CHECK (financial_mode = 'RECORD_ONLY'),
    legal_consent_ver   VARCHAR(30) NOT NULL DEFAULT '2026_V1',
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'SUSPENDED')),
    created_by          BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 5. Kitty Contribution Ledger
CREATE TABLE IF NOT EXISTS manacommunity.kitty_contribution (
    id                  BIGSERIAL PRIMARY KEY,
    cycle_id            BIGINT NOT NULL REFERENCES manacommunity.kitty_cycle(id) ON DELETE CASCADE,
    member_user_id      BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    period_number       INTEGER NOT NULL,
    expected_amount     NUMERIC(14,2) NOT NULL CHECK (expected_amount >= 0),
    recorded_amount     NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (recorded_amount >= 0),
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (payment_status IN ('PENDING', 'PAID', 'VERIFIED', 'DEFAULTED', 'EXEMPT')),
    external_ref        VARCHAR(150),
    receipt_url         TEXT,
    notes               TEXT,
    verified_by         BIGINT REFERENCES manacommunity.app_user(id),
    verified_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_contrib UNIQUE (cycle_id, member_user_id, period_number)
);

-- 6. Kitty Rotation & Payout Records
CREATE TABLE IF NOT EXISTS manacommunity.kitty_rotation (
    id                  BIGSERIAL PRIMARY KEY,
    cycle_id            BIGINT NOT NULL REFERENCES manacommunity.kitty_cycle(id) ON DELETE CASCADE,
    beneficiary_user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    period_number       INTEGER NOT NULL,
    rotation_status     VARCHAR(20) NOT NULL DEFAULT 'PLANNED'
                        CHECK (rotation_status IN ('PLANNED', 'DISBURSED', 'SKIPPED', 'SWAPPED')),
    disbursed_at        TIMESTAMP,
    external_payout_ref VARCHAR(150),
    payout_receipt_url  TEXT,
    CONSTRAINT uk_kitty_rot_period UNIQUE (cycle_id, period_number),
    CONSTRAINT uk_kitty_rot_user UNIQUE (cycle_id, beneficiary_user_id)
);

-- 7. Party Expense Split
CREATE TABLE IF NOT EXISTS manacommunity.kitty_expense_share (
    id                  BIGSERIAL PRIMARY KEY,
    expense_id          BIGINT NOT NULL REFERENCES manacommunity.event_expense(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    share_amount        NUMERIC(14,2) NOT NULL CHECK (share_amount >= 0),
    settlement_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (settlement_status IN ('PENDING', 'SETTLED', 'WAIVED')),
    settled_at          TIMESTAMP,
    settlement_ref      VARCHAR(150),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_expense_user UNIQUE (expense_id, user_id)
);

-- 8. Mana Play: Housie Game Engine Tables
CREATE TABLE IF NOT EXISTS manacommunity.kitty_game_room (
    id                  BIGSERIAL PRIMARY KEY,
    circle_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    room_name           VARCHAR(150) NOT NULL,
    game_type           VARCHAR(30) NOT NULL DEFAULT 'TAMBOLA_90',
    status              VARCHAR(30) NOT NULL DEFAULT 'WAITING'
                        CHECK (status IN ('WAITING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    ticket_price        NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (ticket_price = 0.00),
    max_players         INTEGER DEFAULT 50,
    created_by          BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.kitty_game_player (
    id                  BIGSERIAL PRIMARY KEY,
    room_id             BIGINT NOT NULL REFERENCES manacommunity.kitty_game_room(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    joined_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_game_player UNIQUE (room_id, user_id)
);

CREATE TABLE IF NOT EXISTS manacommunity.kitty_game_ticket (
    id                  BIGSERIAL PRIMARY KEY,
    room_id             BIGINT NOT NULL REFERENCES manacommunity.kitty_game_room(id) ON DELETE CASCADE,
    player_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_game_player(id) ON DELETE CASCADE,
    ticket_matrix       TEXT NOT NULL, -- JSON serialized 3x9 integer grid
    ticket_hash         VARCHAR(64) NOT NULL,
    generated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_game_ticket_hash UNIQUE (room_id, ticket_hash)
);

CREATE TABLE IF NOT EXISTS manacommunity.kitty_game_number_call (
    id                  BIGSERIAL PRIMARY KEY,
    room_id             BIGINT NOT NULL REFERENCES manacommunity.kitty_game_room(id) ON DELETE CASCADE,
    sequence_num        INTEGER NOT NULL,
    called_number       INTEGER NOT NULL CHECK (called_number BETWEEN 1 AND 90),
    called_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_game_call_seq UNIQUE (room_id, sequence_num),
    CONSTRAINT uk_kitty_game_call_num UNIQUE (room_id, called_number)
);

CREATE TABLE IF NOT EXISTS manacommunity.kitty_game_winner (
    id                  BIGSERIAL PRIMARY KEY,
    room_id             BIGINT NOT NULL REFERENCES manacommunity.kitty_game_room(id) ON DELETE CASCADE,
    ticket_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_game_ticket(id),
    user_id             BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    claim_type          VARCHAR(40) NOT NULL
                        CHECK (claim_type IN ('EARLY_FIVE', 'FOUR_CORNERS', 'TOP_LINE', 'MIDDLE_LINE', 'BOTTOM_LINE', 'FULL_HOUSE')),
    claim_status        VARCHAR(30) NOT NULL DEFAULT 'VALIDATED'
                        CHECK (claim_status IN ('PENDING', 'VALIDATED', 'BOGUS')),
    claimed_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    validated_at        TIMESTAMP,
    CONSTRAINT uk_kitty_game_prize_claim UNIQUE (room_id, claim_type)
);
