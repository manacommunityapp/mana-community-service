-- V171__trip_split.sql
-- Mana Trip Split: group trip expenses, split engine inputs/outputs, settlement payments, budget,
-- and per-user My Money integration preference. All money columns are integer paise.

ALTER TABLE manacommunity.community_trip
    ADD COLUMN IF NOT EXISTS organizer_user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_community_trip_organizer ON manacommunity.community_trip(organizer_user_id);

CREATE TABLE IF NOT EXISTS manacommunity.trip_expense (
    id               BIGSERIAL PRIMARY KEY,
    trip_id          VARCHAR(50)  NOT NULL REFERENCES manacommunity.community_trip(id),
    category_code    VARCHAR(30)  NOT NULL,
    description      VARCHAR(255) NOT NULL,
    total_paise      BIGINT       NOT NULL CHECK (total_paise > 0),
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    paid_by_user_id  BIGINT       NOT NULL,
    expense_date     DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | UNSPLIT | VOIDED
    split_method     VARCHAR(20),                               -- EQUAL | PERCENTAGE | EXACT | QUANTITY | SHARES (null while UNSPLIT)
    receipt_url      VARCHAR(1000),
    created_by       BIGINT       NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trip_expense_trip  ON manacommunity.trip_expense(trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_expense_payer ON manacommunity.trip_expense(paid_by_user_id);

-- The inputs the user chose for each participant (weight / quantity / percentage / exact amount).
CREATE TABLE IF NOT EXISTS manacommunity.trip_expense_participant (
    id             BIGSERIAL PRIMARY KEY,
    expense_id     BIGINT NOT NULL REFERENCES manacommunity.trip_expense(id) ON DELETE CASCADE,
    user_id        BIGINT NOT NULL,
    weight         BIGINT NOT NULL DEFAULT 1,
    quantity       BIGINT NOT NULL DEFAULT 1,
    percentage_bp  BIGINT NOT NULL DEFAULT 0,   -- basis points, 10000 = 100%
    exact_paise    BIGINT NOT NULL DEFAULT 0,
    UNIQUE (expense_id, user_id)
);

-- The computed output: what each person owes for the expense. Sum(share_paise) == expense.total_paise.
CREATE TABLE IF NOT EXISTS manacommunity.trip_expense_split (
    id           BIGSERIAL PRIMARY KEY,
    expense_id   BIGINT NOT NULL REFERENCES manacommunity.trip_expense(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL,
    share_paise  BIGINT NOT NULL CHECK (share_paise >= 0),
    UNIQUE (expense_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_trip_expense_split_user ON manacommunity.trip_expense_split(user_id);

-- Real reimbursements between participants. Only CONFIRMED rows affect balances.
CREATE TABLE IF NOT EXISTS manacommunity.trip_settlement_payment (
    id            BIGSERIAL PRIMARY KEY,
    trip_id       VARCHAR(50) NOT NULL REFERENCES manacommunity.community_trip(id),
    from_user_id  BIGINT      NOT NULL,
    to_user_id    BIGINT      NOT NULL,
    amount_paise  BIGINT      NOT NULL CHECK (amount_paise > 0),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',      -- PENDING | CONFIRMED | REJECTED
    method        VARCHAR(30),
    reference     VARCHAR(100),
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    confirmed_at  TIMESTAMP,
    CHECK (from_user_id <> to_user_id)
);

CREATE INDEX IF NOT EXISTS idx_trip_settlement_payment_trip ON manacommunity.trip_settlement_payment(trip_id);

CREATE TABLE IF NOT EXISTS manacommunity.trip_budget (
    trip_id          VARCHAR(50) PRIMARY KEY REFERENCES manacommunity.community_trip(id),
    estimated_paise  BIGINT      NOT NULL CHECK (estimated_paise >= 0),
    currency         VARCHAR(10) NOT NULL DEFAULT 'INR',
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Opt-in My Money integration, per user per trip. Default (no row) is OFF.
-- mode: OFF | SHARE (record my share of each expense) | SETTLEMENTS (record settlement payments I send/receive).
-- One mode at a time on purpose: recording both would double-count the same money.
CREATE TABLE IF NOT EXISTS manacommunity.trip_my_money_pref (
    id        BIGSERIAL PRIMARY KEY,
    trip_id   VARCHAR(50) NOT NULL REFERENCES manacommunity.community_trip(id),
    user_id   BIGINT      NOT NULL,
    mode      VARCHAR(20) NOT NULL DEFAULT 'OFF',
    UNIQUE (trip_id, user_id)
);
