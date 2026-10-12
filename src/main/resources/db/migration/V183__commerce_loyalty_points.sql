-- V183: Commerce Loyalty Points — earn on purchase, redeem on checkout

CREATE TABLE IF NOT EXISTS commerce_loyalty_account (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    community_id        BIGINT NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    total_points_earned BIGINT NOT NULL DEFAULT 0,
    total_points_spent  BIGINT NOT NULL DEFAULT 0,
    available_points    BIGINT NOT NULL DEFAULT 0,
    tier                VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_user ON commerce_loyalty_account(user_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_community ON commerce_loyalty_account(community_id);

CREATE TABLE IF NOT EXISTS commerce_loyalty_transaction (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES commerce_loyalty_account(id) ON DELETE CASCADE,
    order_number    VARCHAR(40),
    type            VARCHAR(20) NOT NULL,  -- EARN | REDEEM | EXPIRE | ADJUST
    points          BIGINT NOT NULL,       -- positive = earn, negative = deduct
    balance_after   BIGINT NOT NULL,
    description     VARCHAR(200),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_txn_account ON commerce_loyalty_transaction(account_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_order ON commerce_loyalty_transaction(order_number);
