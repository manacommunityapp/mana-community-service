-- Payments: receipts, reconciliation, dispute tracking, payment request links, rewards

CREATE TABLE payment_receipt (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    source_module   VARCHAR(100) NOT NULL,
    reference_id    BIGINT NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_method  VARCHAR(100),
    gateway_ref     VARCHAR(200),
    receipt_number  VARCHAR(100) UNIQUE NOT NULL,
    paid_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prec_community ON payment_receipt(community_id);
CREATE INDEX idx_prec_user ON payment_receipt(user_id);

CREATE TABLE payment_reconciliation (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    total_collected NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_refunded  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_pending   NUMERIC(12,2) NOT NULL DEFAULT 0,
    discrepancy     NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMP WITH TIME ZONE,
    notes           VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, period_start, period_end)
);
CREATE INDEX idx_recon_community ON payment_reconciliation(community_id);

CREATE TABLE payment_dispute (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    receipt_id      BIGINT REFERENCES payment_receipt(id),
    amount          NUMERIC(12,2) NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    description     VARCHAR(2000),
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    assigned_to     BIGINT,
    resolution      VARCHAR(2000),
    resolved_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pd_community ON payment_dispute(community_id);
CREATE INDEX idx_pd_user ON payment_dispute(user_id);

CREATE TABLE payment_request_link (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    created_by      BIGINT NOT NULL,
    payee_user_id   BIGINT,
    amount          NUMERIC(12,2) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    link_token      VARCHAR(64) UNIQUE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE,
    paid            BOOLEAN NOT NULL DEFAULT FALSE,
    paid_at         TIMESTAMP WITH TIME ZONE,
    paid_by         BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prl_community ON payment_request_link(community_id);
CREATE INDEX idx_prl_token ON payment_request_link(link_token);

CREATE TABLE payment_reward (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    source_module   VARCHAR(100) NOT NULL,
    reference_id    BIGINT,
    reward_type     VARCHAR(100) NOT NULL,
    points          INT NOT NULL DEFAULT 0,
    description     VARCHAR(500),
    expires_at      TIMESTAMP WITH TIME ZONE,
    redeemed        BOOLEAN NOT NULL DEFAULT FALSE,
    redeemed_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_preward_community ON payment_reward(community_id);
CREATE INDEX idx_preward_user ON payment_reward(user_id);
