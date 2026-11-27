-- ─────────────────────────────────────────────────────────────────────────────
-- V509: Create business_module_finance_daily and community_saas_subscription.
--
-- V100 and V101 were previously defined with version numbers < 115, but databases
-- baselined at V115 skip migrations <= 115. This migration ensures both tables are
-- created on all existing environments.
-- ─────────────────────────────────────────────────────────────────────────────

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

CREATE TABLE IF NOT EXISTS business_module_finance_daily (
    id BIGSERIAL PRIMARY KEY,
    module_code VARCHAR(60) NOT NULL,
    report_date DATE NOT NULL,
    commission_revenue NUMERIC(14,2) NOT NULL,
    direct_cost NUMERIC(14,2) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_business_module_finance_daily UNIQUE (module_code, report_date)
);

CREATE INDEX IF NOT EXISTS idx_business_module_finance_daily_date
    ON business_module_finance_daily(report_date);
