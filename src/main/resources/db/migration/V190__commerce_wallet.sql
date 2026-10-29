-- Phase 20: Digital Wallet (MANA Pay)
CREATE TABLE IF NOT EXISTS commerce_wallet (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES app_user(id),
    community_id    BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    balance         NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    currency        VARCHAR(5)   NOT NULL DEFAULT 'INR',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE FROZEN
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, community_id)
);

CREATE TABLE IF NOT EXISTS commerce_wallet_transaction (
    id              BIGSERIAL PRIMARY KEY,
    wallet_id       BIGINT       NOT NULL REFERENCES commerce_wallet(id),
    txn_ref         VARCHAR(60)  NOT NULL UNIQUE,
    txn_type        VARCHAR(20)  NOT NULL,  -- TOPUP PAYMENT REFUND CASHBACK ADJUSTMENT
    amount          NUMERIC(14,2) NOT NULL,
    balance_after   NUMERIC(14,2) NOT NULL,
    reference_id    VARCHAR(80),            -- order_number / razorpay_ref
    description     VARCHAR(300),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wallet_user      ON commerce_wallet (user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_wallet ON commerce_wallet_transaction (wallet_id);
