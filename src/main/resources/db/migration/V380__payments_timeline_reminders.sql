-- Payments: shared timeline, bill reminders, approved mandates

CREATE TABLE payment_timeline_event (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    source_module   VARCHAR(100) NOT NULL,
    reference_id    BIGINT NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    amount          NUMERIC(12,2),
    description     VARCHAR(500),
    metadata        JSONB,
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pte_community ON payment_timeline_event(community_id);
CREATE INDEX idx_pte_user ON payment_timeline_event(user_id);
CREATE INDEX idx_pte_occurred ON payment_timeline_event(occurred_at DESC);

CREATE TABLE bill_reminder (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    bill_type       VARCHAR(100) NOT NULL,
    biller_name     VARCHAR(200) NOT NULL,
    amount          NUMERIC(12,2),
    due_date        DATE NOT NULL,
    reminder_days   INT NOT NULL DEFAULT 3,
    reminder_sent   BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_at TIMESTAMP WITH TIME ZONE,
    paid            BOOLEAN NOT NULL DEFAULT FALSE,
    paid_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_br_community ON bill_reminder(community_id);
CREATE INDEX idx_br_user ON bill_reminder(user_id);
CREATE INDEX idx_br_due ON bill_reminder(due_date);

CREATE TABLE payment_mandate (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    mandate_type    VARCHAR(100) NOT NULL,
    provider        VARCHAR(200) NOT NULL,
    max_amount      NUMERIC(12,2) NOT NULL,
    frequency       VARCHAR(50) NOT NULL,
    approved_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    cancelled_at    TIMESTAMP WITH TIME ZONE,
    cancel_reason   VARCHAR(500)
);
CREATE INDEX idx_pm_community ON payment_mandate(community_id);
CREATE INDEX idx_pm_user ON payment_mandate(user_id);
