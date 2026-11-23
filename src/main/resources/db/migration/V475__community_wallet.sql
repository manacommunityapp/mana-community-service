-- Community wallet: per-resident in-community wallet for maintenance payments and embedded credit
CREATE TABLE community_resident_wallets (
  id               BIGSERIAL PRIMARY KEY,
  resident_id      BIGINT NOT NULL,
  community_id     BIGINT NOT NULL,
  balance          NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  credit_limit     NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  currency         VARCHAR(10) NOT NULL DEFAULT 'INR',
  status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                   -- ACTIVE, FROZEN, CLOSED
  upi_virtual_id   VARCHAR(100),   -- stub: resident@mana.pay (real requires PA licence)
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(resident_id, community_id)
);

-- Append-only double-entry ledger; never UPDATE or DELETE rows
CREATE TABLE community_wallet_transactions (
  id               BIGSERIAL PRIMARY KEY,
  wallet_id        BIGINT NOT NULL REFERENCES community_resident_wallets(id),
  community_id     BIGINT NOT NULL,
  txn_type         VARCHAR(50) NOT NULL,
                   -- MAINTENANCE_PAYMENT, TOPUP, REFUND, GROUP_EXPENSE_SETTLE,
                   --  AMENITY_BOOKING, VENDOR_PAYMENT, CREDIT_DRAWDOWN, CREDIT_REPAYMENT
  direction        VARCHAR(10) NOT NULL,  -- DEBIT, CREDIT
  amount           NUMERIC(12,2) NOT NULL,
  balance_after    NUMERIC(12,2) NOT NULL,
  reference_type   VARCHAR(50),
  reference_id     BIGINT,
  description      VARCHAR(500),
  payment_method   VARCHAR(50),    -- WALLET, UPI, BBPS, NETBANKING, CARD
  external_txn_ref VARCHAR(200),
  status           VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
                   -- PENDING, COMPLETED, FAILED, REVERSED
  initiated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at     TIMESTAMPTZ
);

-- Committee-approved credit lines for maintenance fund advances
CREATE TABLE community_wallet_credit_lines (
  id                BIGSERIAL PRIMARY KEY,
  wallet_id         BIGINT NOT NULL REFERENCES community_resident_wallets(id),
  approved_by       BIGINT NOT NULL,
  approved_amount   NUMERIC(12,2) NOT NULL,
  interest_rate_pct NUMERIC(5,2) NOT NULL DEFAULT 0.00,
  repayment_days    INT NOT NULL DEFAULT 90,
  drawn_amount      NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  approved_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at        TIMESTAMPTZ
);

CREATE INDEX idx_comm_wallet_resident ON community_resident_wallets(resident_id, community_id);
CREATE INDEX idx_comm_wallet_txn_wallet ON community_wallet_transactions(wallet_id, initiated_at DESC);
CREATE INDEX idx_comm_wallet_txn_community ON community_wallet_transactions(community_id, txn_type, initiated_at DESC);
