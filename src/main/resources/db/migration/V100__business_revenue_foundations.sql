CREATE TABLE IF NOT EXISTS community_saas_subscription (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES community(id),
    external_reference VARCHAR(120) UNIQUE,
    plan_code VARCHAR(60) NOT NULL,
    plan_name VARCHAR(120) NOT NULL,
    recurring_amount NUMERIC(14,2) NOT NULL CHECK (recurring_amount >= 0),
    billing_period_months INTEGER NOT NULL CHECK (billing_period_months IN (1, 3, 6, 12)),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','TRIALING','PAST_DUE','PAUSED','CANCELLED','EXPIRED')),
    current_period_start DATE NOT NULL,
    current_period_end DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (current_period_end IS NULL OR current_period_end >= current_period_start)
);

CREATE INDEX IF NOT EXISTS idx_community_saas_subscription_status_period
    ON community_saas_subscription(status, current_period_end);

CREATE UNIQUE INDEX IF NOT EXISTS uq_community_saas_subscription_active_community
    ON community_saas_subscription(community_id)
    WHERE status IN ('ACTIVE','TRIALING','PAST_DUE');
